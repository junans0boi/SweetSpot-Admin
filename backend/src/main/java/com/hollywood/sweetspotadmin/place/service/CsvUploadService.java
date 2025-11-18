package com.hollywood.sweetspotadmin.place.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator; // ✅ [추가] Iterator

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvUploadService {

    private final JobStatusService jobStatusService;

    @Qualifier("domainJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    @Async
    public void loadAllCsvFilesFromDisk(String jobId, String directoryPath) {
        try {
            File dir = new File(directoryPath);
            File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));

            if (csvFiles == null || csvFiles.length == 0) {
                jobStatusService.updateStatus(jobId, "FAILED: 지정된 경로에 CSV 파일이 없습니다.");
                return;
            }

            long totalProcessedCount = 0;
            long totalSuccessCount = 0;
            int totalFiles = csvFiles.length;

            log.info("[CsvUploadService] 기존 Raw 테이블(places_raw) 데이터를 비웁니다.");
            jdbcTemplate.execute("TRUNCATE TABLE places_raw");
            log.info("[CsvUploadService] Raw 테이블 비우기 완료.");
            
            // ✅ [FINAL] CSVFormat 정의 (깨진 헤더 및 줄바꿈이 포함된 따옴표 필드 지원)
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true) // 빈 줄은 무시
                    .setAllowMissingColumnNames(true) // "...," 같은 헤더 오류 무시
                    .setTrim(true)
                    .build();

            for (int i = 0; i < totalFiles; i++) {
                File file = csvFiles[i];
                String statusMessage = String.format("파일 처리 중 (%d/%d): %s", i + 1, totalFiles, file.getName());
                log.info("===== {} =====", statusMessage);
                jobStatusService.updateStatus(jobId, statusMessage);

                List<PlaceRaw> batchList = new ArrayList<>();

                try (Reader reader = new FileReader(file, StandardCharsets.UTF_8);
                     CSVParser csvParser = new CSVParser(reader, csvFormat)) {

                    Map<String, Integer> headerMap = csvParser.getHeaderMap();
                    String subCategoryHeader = findSubCategoryHeader(headerMap);

                    int rowCount = 1;
                    
                    // ✅ [FINAL] Iterator 버그 수정
                    Iterator<CSVRecord> recordIterator = csvParser.iterator();

                    while (true) {
                        CSVRecord record = null;
                        try {
                            // 1. 다음 레코드를 가져오려고 시도
                            if (!recordIterator.hasNext()) { // ⬅️ 수정됨
                                break; // 파일 끝, 루프 종료
                            }
                            record = recordIterator.next(); // ⬅️ 수정됨
                            rowCount++;
                            totalProcessedCount++;

                            // 2. 레코드 파싱 (기존 로직)
                            PlaceRaw placeRaw = parseLineToRaw(record, headerMap, subCategoryHeader);
                            if (placeRaw != null) {
                                batchList.add(placeRaw);
                            }

                            // 3. 배치 처리 (기존 로직)
                            if (batchList.size() >= BATCH_SIZE) {
                                saveBatchRaw(batchList);
                                totalSuccessCount += batchList.size();
                                batchList.clear();

                                statusMessage = String.format("파일 처리 중 (%d/%d): %s (현재 %d 행 처리)",
                                        i + 1, totalFiles, file.getName(), rowCount);
                                jobStatusService.updateStatus(jobId, statusMessage);
                            }
                        } catch (java.io.UncheckedIOException | java.lang.IllegalArgumentException e) {
                            // 🚨 [핵심] CSV 파싱 오류를 여기서 잡고 건너뜁니다.
                            log.warn("CSV 데이터 파싱 중 심각한 오류 발생 ({}번째 줄 스킵): {}", rowCount, e.getMessage());
                        } catch (Exception e) {
                            // 🚨 PlaceRaw 생성 또는 배치 저장 중 발생하는 예기치 못한 오류
                            log.error("CSV 처리 중 예기치 못한 오류 ({}번째 줄 스킵): {}", rowCount, record != null ? record.toString() : "N/A", e);
                        }
                    } // end while

                    // 남은 데이터 처리
                    if (!batchList.isEmpty()) {
                        saveBatchRaw(batchList);
                        totalSuccessCount += batchList.size();
                    }

                } catch (Exception e) {
                    log.error("파일 처리 중 심각한 오류 발생 (파일 건너뜀): {}", file.getName(), e);
                }
            } // end for

            log.info("[CsvUploadService] 모든 Raw 데이터 적재 완료.");
            jobStatusService.updateStatus(jobId, "COMPLETED: 모든 파일 적재 완료. (DB 변환은 백그라운드에서 자동 실행됩니다)");

        } catch (Exception e) {
            log.error("CSV 일괄 처리 실패", e);
            jobStatusService.updateStatus(jobId, "FAILED: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "domainTransactionManager")
    public void saveBatchRaw(List<PlaceRaw> batchList) {
        if (batchList.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO places_raw (id, name, main_category, sub_category, " +
                "road_address, jibun_address, epsg5174x, epsg5174y, trade_state_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PlaceRaw raw = batchList.get(i);
                ps.setString(1, raw.id);
                ps.setString(2, raw.name);
                ps.setString(3, raw.mainCategory);
                ps.setString(4, raw.subCategory);
                ps.setString(5, raw.roadAddress);
                ps.setString(6, raw.jibunAddress);
                ps.setString(7, raw.epsg5174x);
                ps.setString(8, raw.epsg5174y);
                ps.setString(9, raw.tradeStateName);
            }
            @Override
            public int getBatchSize() {
                return batchList.size();
            }
        });
    }

    private String getRecordValue(CSVRecord record, Map<String, Integer> headerMap, String headerName) {
        if (headerMap.containsKey(headerName) && record.isMapped(headerName)) {
            return record.get(headerName);
        }
        return null;
    }

    private Map<String, Integer> parseHeader(String[] headers) {
        return new HashMap<>();
    }
    
    private String findSubCategoryHeader(Map<String, Integer> headerMap) {
        if (headerMap.containsKey("업태구분명")) return "업태구분명";
        if (headerMap.containsKey("위생업태명")) return "위생업태명";
        if (headerMap.containsKey("문화체육업종명")) return "문화체육업종명";
        return null;
    }

    private PlaceRaw parseLineToRaw(CSVRecord record, Map<String, Integer> headerMap, String subCategoryHeader) {
        
        String idStr = getRecordValue(record, headerMap, "번호");
        String name = getRecordValue(record, headerMap, "사업장명");
        String mainCategory = getRecordValue(record, headerMap, "개방서비스명");
        String tradeStateName = getRecordValue(record, headerMap, "영업상태명");

        if (!StringUtils.hasText(idStr) || !StringUtils.hasText(name) || !StringUtils.hasText(mainCategory) || !StringUtils.hasText(tradeStateName)) {
            return null;
        }

        if (!"영업/정상".equals(tradeStateName)) {
            return null;
        }

        String roadAddress = getRecordValue(record, headerMap, "도로명전체주소");
        String jibunAddress = getRecordValue(record, headerMap, "소재지전체주소");

        if (!StringUtils.hasText(roadAddress) && !StringUtils.hasText(jibunAddress)) {
            return null;
        }

        String subCategory = "";
        if (subCategoryHeader != null) {
            subCategory = getRecordValue(record, headerMap, subCategoryHeader);
        }
        if (!StringUtils.hasText(subCategory)) {
            subCategory = mainCategory;
        }

        String latitudeStr = getRecordValue(record, headerMap, "좌표정보y(epsg5174)");
        String longitudeStr = getRecordValue(record, headerMap, "좌표정보x(epsg5174)");

        return new PlaceRaw(
            idStr, name, mainCategory, subCategory, 
            roadAddress, jibunAddress,
            longitudeStr, latitudeStr,
            tradeStateName
        );
    }
    
    private static class PlaceRaw {
        final String id;
        final String name;
        final String mainCategory;
        final String subCategory;
        final String roadAddress;
        final String jibunAddress;
        final String epsg5174x;
        final String epsg5174y;
        final String tradeStateName;

        PlaceRaw(String id, String name, String mainCategory, String subCategory,
                String roadAddress, String jibunAddress, 
                String epsg5174x, String epsg5174y, String tradeStateName) {
            this.id = id;
            this.name = name;
            this.mainCategory = mainCategory;
            this.subCategory = subCategory;
            this.roadAddress = roadAddress;
            this.jibunAddress = jibunAddress;
            this.epsg5174x = epsg5174x;
            this.epsg5174y = epsg5174y;
            this.tradeStateName = tradeStateName;
        }
    }
}
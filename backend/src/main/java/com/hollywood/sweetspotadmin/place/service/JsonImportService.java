package com.hollywood.sweetspotadmin.place.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollywood.sweetspotadmin.place.model.Place;
import com.hollywood.sweetspotadmin.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonImportService {

    private final PlaceRepository placeRepository;
    private final JobStatusService jobStatusService;
    private final ObjectMapper objectMapper;
    private final CoordinateTransform coordinateTransform;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int BATCH_SIZE = 1000;

    @Async
    public void importFromJsonFile(String jobId, String projectRootPath) {
        File jsonFile = Paths.get(projectRootPath, "output", "data.json").toFile();
        File errorLogFile = Paths.get(projectRootPath, "logs", "failed_records.log").toFile();

        jobStatusService.updateStatus(jobId, "JSON 임포트 시작: " + jsonFile.getName());
        log.info("[JsonImportService] Starting JSON import for job ID: {}", jobId);

        if (!jsonFile.exists()) {
            String errorMessage = "FAILED: JSON 파일을 찾을 수 없습니다. 경로: " + jsonFile.getAbsolutePath();
            log.error("[JsonImportService] {}", errorMessage);
            jobStatusService.updateStatus(jobId, errorMessage);
            return;
        }

        try (FileWriter errorWriter = new FileWriter(errorLogFile, false)) {
            JsonFactory factory = objectMapper.getFactory();
            try (JsonParser parser = factory.createParser(jsonFile)) {

                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new IOException("JSON 파일이 배열로 시작하지 않습니다.");
                }

                List<Place> batchList = new ArrayList<>();
                int totalCount = 0;
                int processedCount = 0;
                int successCount = 0;
                int failedCount = 0;

                // Read records one by one
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    totalCount++;
                    processedCount++;

                    Map<String, String> record = parser.readValueAs(new TypeReference<>() {});

                    try {
                        // 1. Filter record
                        String tradeStateName = record.get("tradeStateName");
                        if (!"영업/정상".equals(tradeStateName)) {
                            throw new RuntimeException("영업상태가 '영업/정상'이 아님: " + tradeStateName);
                        }

                        String roadAddress = record.get("roadAddress");
                        String jibunAddress = record.get("jibunAddress");
                        if (!StringUtils.hasText(roadAddress) && !StringUtils.hasText(jibunAddress)) {
                            throw new RuntimeException("도로명주소와 지번주소가 모두 없음");
                        }

                        // 2. Transform to Place entity
                        Place place = transformToPlace(record);
                        batchList.add(place);
                        successCount++;

                        // 3. Save in batches
                        if (batchList.size() >= BATCH_SIZE) {
                            placeRepository.saveAll(batchList);
                            log.info("[JsonImportService] Saved a batch of {} places.", batchList.size());
                            jobStatusService.updateStatus(jobId, String.format("처리 중... (%d건 처리)", processedCount));
                            batchList.clear();
                        }
                    } catch (Exception e) {
                        failedCount++;
                        String errorReason = "Reason: " + e.getMessage();
                        String recordJson = objectMapper.writeValueAsString(record);
                        errorWriter.write(errorReason + " | " + recordJson + "\n");
                    }
                }

                // Save remaining batch
                if (!batchList.isEmpty()) {
                    placeRepository.saveAll(batchList);
                    log.info("[JsonImportService] Saved the final batch of {} places.", batchList.size());
                }

                String finalStatus = String.format("COMPLETED: 총 %d개 중 %d개 성공, %d개 실패. (상세내용: logs/failed_records.log)",
                        totalCount, successCount, failedCount);
                jobStatusService.updateStatus(jobId, finalStatus);
                log.info("[JsonImportService] {}", finalStatus);
            }
        } catch (IOException e) {
            String errorMessage = "FAILED: JSON 파일 처리 중 심각한 오류 발생. " + e.getMessage();
            log.error("[JsonImportService] {}", errorMessage, e);
            jobStatusService.updateStatus(jobId, errorMessage);
        }
    }

    private Place transformToPlace(Map<String, String> record) {
        Place place = new Place();

                // place.setId(Long.parseLong(record.get("id"))); // Let DB generate ID
        place.setName(record.get("name"));
        place.setMainCategory(record.get("mainCategory"));
        
        String subCategory = record.get("subCategory");
        place.setSubCategory(StringUtils.hasText(subCategory) ? subCategory : record.get("mainCategory"));

        String roadAddress = record.get("roadAddress");
        String jibunAddress = record.get("jibunAddress");
        place.setAddress(StringUtils.hasText(roadAddress) ? roadAddress : jibunAddress);
        place.setRoadAddress(roadAddress);
        place.setJibunAddress(jibunAddress);

        // Coordinate transformation
        try {
            String epsgX = record.get("epsg5174x");
            String epsgY = record.get("epsg5174y");

            if (StringUtils.hasText(epsgX) && StringUtils.hasText(epsgY)) {
                double x = Double.parseDouble(epsgX);
                double y = Double.parseDouble(epsgY);

                place.setEpsg5174x(x);
                place.setEpsg5174y(y);

                ProjCoordinate srcCoord = new ProjCoordinate(x, y);
                ProjCoordinate dstCoord = new ProjCoordinate();
                coordinateTransform.transform(srcCoord, dstCoord);

                Point point = geometryFactory.createPoint(new Coordinate(dstCoord.x, dstCoord.y));
                place.setGeom(point);
            }
        } catch (Exception e) {
            log.warn("[JsonImportService] 좌표 변환 실패 (ID: {}): {}", record.get("id"), e.getMessage());
            place.setGeom(null);
        }

        place.setRating(0.0);
        place.setDetailsCached(false);

        return place;
    }
}

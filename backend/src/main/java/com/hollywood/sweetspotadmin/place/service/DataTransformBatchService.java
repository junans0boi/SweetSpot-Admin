package com.hollywood.sweetspotadmin.place.service;

import com.hollywood.sweetspotadmin.place.model.Place;
import com.hollywood.sweetspotadmin.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.proj4j.CoordinateTransform; // ✅ [추가]
import org.locationtech.proj4j.ProjCoordinate; // ✅ [추가]
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // ✅ [추가]

@Slf4j
@Service
//@EnableScheduling
//@EnableAsync
@RequiredArgsConstructor
public class DataTransformBatchService {

    @Qualifier("domainJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;
    private final PlaceRepository placeRepository;
    
    // ✅ [추가] Java 좌표 변환기 (PostGIS 버그 우회)
    private final CoordinateTransform coordinateTransform; 
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private static final int BATCH_LIMIT = 10000; // ⬅️ 10분당 1만건 처리

    /**
     * [2단계: 변환] 10분 간격으로 places_raw의 데이터를 Java로 변환하여 places에 저장
     */
    @Async
    //    @Scheduled(fixedDelay = 300000) // 5분
    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "domainTransactionManager")
    public void transformRawToProductionBatch() {
        log.info("[TransformBatch] Raw -> Production 변환 배치를 시작합니다.");

        String selectSql = "SELECT * FROM places_raw LIMIT " + BATCH_LIMIT;
        
        // 1. Raw 테이블에서 50,000건 조회
        List<PlaceRawDTO> rawData = jdbcTemplate.query(selectSql, (rs, rowNum) -> new PlaceRawDTO(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("main_category"),
                rs.getString("sub_category"),
                rs.getString("road_address"),
                rs.getString("jibun_address"),
                rs.getString("epsg5174x"),
                rs.getString("epsg5174y")
        ));

        if (rawData.isEmpty()) {
            log.info("[TransformBatch] 변환할 데이터가 없습니다. (places_raw 비어있음)");
            return;
        }

        try {
            List<Place> placesToSave = new ArrayList<>();
            List<Long> processedIds = new ArrayList<>(); // ✅ ID 타입을 Long으로 변경

            // 2. Java에서 데이터 변환 (PostGIS 버그 우회)
            for (PlaceRawDTO raw : rawData) {
                Long currentId;
                try {
                    currentId = Long.parseLong(raw.id);
                } catch (NumberFormatException e) {
                    log.warn("[TransformBatch] ID 파싱 실패 (ID: {}), 이 레코드를 건너뜁니다.", raw.id);
                    continue; // 이 레코드는 처리하지 않음 (삭제도 하지 않음)
                }
                
                processedIds.add(currentId); // 삭제할 ID 목록에 추가

                Place place = new Place();
                place.setId(currentId);
                place.setName(raw.name);
                place.setMainCategory(raw.mainCategory);
                place.setSubCategory(StringUtils.hasText(raw.subCategory) ? raw.subCategory : raw.mainCategory);
                
                String roadAddress = raw.roadAddress;
                String jibunAddress = raw.jibunAddress;
                place.setAddress(StringUtils.hasText(roadAddress) ? roadAddress : jibunAddress);
                place.setRoadAddress(roadAddress);
                place.setJibunAddress(jibunAddress);

                // Java 좌표 변환
                try {
                    String epsgX = raw.epsg5174x;
                    String epsgY = raw.epsg5174y;

                    if (StringUtils.hasText(epsgX) && StringUtils.hasText(epsgY)) {
                        double x = Double.parseDouble(epsgX);
                        double y = Double.parseDouble(epsgY);
                        
                        place.setEpsg5174x(x);
                        place.setEpsg5174y(y);

                        ProjCoordinate srcCoord = new ProjCoordinate(x, y);
                        ProjCoordinate dstCoord = new ProjCoordinate();
                        coordinateTransform.transform(srcCoord, dstCoord); // WGS84로 변환

                        Point point = geometryFactory.createPoint(new Coordinate(dstCoord.x, dstCoord.y));
                        place.setGeom(point);
                    }
                } catch (Exception e) { // Proj4j 또는 NumberFormat 예외 모두 처리
                    log.warn("[TransformBatch] 좌표 변환/파싱 실패 (ID: {})", place.getId(), e.getMessage());
                    place.setGeom(null);
                }
                
                place.setRating(0.0);
                place.setDetailsCached(false);
                
                placesToSave.add(place);
            }

            // 3. JPA saveAll을 사용하여 Bulk Insert/Update (ON CONFLICT 역할)
            placeRepository.saveAll(placesToSave);
            log.info("[TransformBatch] {}건의 데이터를 'places' 테이블로 변환/저장했습니다.", placesToSave.size());

            // 4. 변환된 데이터를 'places_raw'에서 삭제
            // [수정] ID 리스트를 SQL의 IN절에 맞게 변환
            String idsForDelete = processedIds.stream()
                                    .map(id -> "'" + id + "'")
                                    .collect(Collectors.joining(","));
            
            // [수정] ID가 0개일 경우 오류 방지
            if (idsForDelete.isEmpty()) {
                log.warn("[TransformBatch] 처리된 ID가 없어 삭제 작업을 건너뜁니다.");
                return;
            }

            String deleteSql = "DELETE FROM places_raw WHERE id IN (" + idsForDelete + ")";
            jdbcTemplate.update(deleteSql);
            
            log.info("[TransformBatch] 변환된 데이터를 'places_raw' 테이블에서 삭제했습니다.");

        } catch (Exception e) {
            log.error("[TransformBatch] DB 내부 변환 중 심각한 오류 발생", e);
        }
    }

    // 1단계: JDBC 조회용 내부 DTO
    private static class PlaceRawDTO {
        final String id;
        final String name;
        final String mainCategory;
        final String subCategory;
        final String roadAddress;
        final String jibunAddress;
        final String epsg5174x;
        final String epsg5174y;

        PlaceRawDTO(String id, String name, String mainCategory, String subCategory,
                    String roadAddress, String jibunAddress,
                    String epsg5174x, String epsg5174y) {
            this.id = id;
            this.name = name;
            this.mainCategory = mainCategory;
            this.subCategory = subCategory;
            this.roadAddress = roadAddress;
            this.jibunAddress = jibunAddress;
            this.epsg5174x = epsg5174x;
            this.epsg5174y = epsg5174y;
        }
    }
}
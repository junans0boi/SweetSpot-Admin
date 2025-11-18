package com.hollywood.sweetspotadmin.place.controller;


import com.hollywood.sweetspotadmin.place.dto.PlaceUpdateRequest; // ✅ [추가]
import com.fasterxml.jackson.core.type.TypeReference; // ✅ [추가]
import com.fasterxml.jackson.databind.ObjectMapper; // ✅ [추가]
import com.hollywood.sweetspotadmin.place.dto.PlaceSummaryDto;
import com.hollywood.sweetspotadmin.place.dto.ReviewDto; // ✅ [추가]
import com.hollywood.sweetspotadmin.place.model.Place;
import com.hollywood.sweetspotadmin.place.repository.PlaceRepository;
import com.hollywood.sweetspotadmin.place.service.CsvUploadService;
import com.hollywood.sweetspotadmin.place.service.JobStatusService;
import com.hollywood.sweetspotadmin.place.service.JsonImportService;
import com.hollywood.sweetspotadmin.place.service.GeocodingBatchService;
import com.hollywood.sweetspotadmin.place.specification.PlaceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional; // ✅ [추가]
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final PlaceRepository placeRepository;
    private final CsvUploadService csvUploadService;
    private final JsonImportService jsonImportService;
    private final JobStatusService jobStatusService;
    private final GeocodingBatchService geocodingBatchService; // ✅ [추가]
    private final ObjectMapper objectMapper;

    @Value("${csv.directory-path}")
    private String csvPath;

    @Value("${project.root-path}")
    private String projectRootPath;

    /**
     * ✅ [수정] 페이지네이션 및 카테고리 필터링 적용
     *
     * @param mainCategory (필터) 예: "일반음식점"
     * @param pageable     (페이지) 예: ?page=0&size=20&sort=id,asc
     * @return
     */
    /**
     * ✅ [수정] Specification을 사용하도록 변경
     */
    @GetMapping
    public ResponseEntity<Page<PlaceSummaryDto>> getAllPlaces(
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String keyword, // ✅ [추가] 검색어
            Pageable pageable) {

        // 1. 동적 쿼리 생성
        Specification<Place> spec = PlaceSpecification.search(mainCategory, keyword);

        // 2. 쿼리 실행
        Page<Place> placePage = placeRepository.findAll(spec, pageable);

        // 3. DTO로 변환
        Page<PlaceSummaryDto> dtoPage = placePage.map(PlaceSummaryDto::from);

        return ResponseEntity.ok(dtoPage);
    }

    /**
     * ✅ [신규] 장소 카테고리 목록 API
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getPlaceCategories() {
        return ResponseEntity.ok(placeRepository.findDistinctMainCategories());
    }

    /**
     * ✅ [신규] JSON 임포트 작업을 시작하고 Job ID를 반환
     */
    @PostMapping("/import-json")
    public ResponseEntity<String> loadDataFromJson() {
        if (jobStatusService.isJobRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 다른 임포트 작업이 진행 중입니다.");
        }
        String jobId = UUID.randomUUID().toString();
        log.info("JSON 임포트 작업 시작. Job ID: {}", jobId);
        jsonImportService.importFromJsonFile(jobId, projectRootPath);
        return ResponseEntity.ok(jobId);
    }

    /**
     * ✅ [신규] 모든 작업 상태 조회 통합
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<String> getJobStatus(@PathVariable String jobId) {
        String status = jobStatusService.getStatus(jobId);
        return ResponseEntity.ok("{\"status\": \"" + status + "\"}");
    }

    /*
    @PostMapping("/load-from-disk")
    public ResponseEntity<String> loadAllCsvFilesFromDisk() {
        // 1. 고유한 작업 ID 생성
        String jobId = UUID.randomUUID().toString();
        log.info("CSV 일괄 적재 작업 시작. Job ID: {}", jobId);

        // 2. 비동기 서비스 호출
        csvUploadService.loadAllCsvFilesFromDisk(jobId, csvPath);

        // 3. 서비스가 완료될 때까지 기다리지 않고, Job ID를 즉시 반환
        return ResponseEntity.ok(jobId);
    }

    @GetMapping("/load-from-disk/status/{jobId}")
    public ResponseEntity<String> getJobStatus(@PathVariable String jobId) {
        String status = jobStatusService.getStatus(jobId);

        // 프론트엔드가 쉽게 파싱하도록 JSON 형태로 반환
        return ResponseEntity.ok("{\"status\": \"" + status + "\"}");
    }
    */

    /**
     * ✅ [신규] 수동으로 지오코딩 배치 작업을 시작합니다.
     */
    @PostMapping("/geocoding/run")
    public ResponseEntity<String> runGeocodingBatch() {
        log.info("수동 지오코딩 배치 작업 시작 요청.");
        geocodingBatchService.processMissingCoordinates();
        return ResponseEntity.ok("지오코딩 배치 작업이 시작되었습니다.");
    }

    /**
     * ✅ [신규] 모든 장소 데이터를 일괄 삭제합니다.
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllPlaces() {
        try {
            long count = placeRepository.count();
            placeRepository.deleteAllInBatch();
            return ResponseEntity.ok(count + "개의 장소 데이터를 모두 삭제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("데이터 삭제 중 오류 발생: " + e.getMessage());
        }
    }


    /**
     * ✅ [신규] 특정 장소의 정보를 수정합니다.
     */
    @PutMapping("/{placeId}")
    @Transactional
    public ResponseEntity<PlaceSummaryDto> updatePlace(
            @PathVariable Long placeId,
            @RequestBody PlaceUpdateRequest request) {
        
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다. ID: " + placeId));

        // DTO의 데이터로 엔티티 업데이트
        place.setName(request.getName());
        place.setAddress(request.getAddress());
        place.setMainCategory(request.getMainCategory());
        place.setSubCategory(request.getSubCategory());
        
        // (좌표는 이 API로 수정하지 않음)

        Place updatedPlace = placeRepository.save(place);
        return ResponseEntity.ok(PlaceSummaryDto.from(updatedPlace));
    }

    /**
     * ✅ [신규] 특정 장소를 삭제합니다.
     */
    @DeleteMapping("/{placeId}")
    @Transactional
    public ResponseEntity<Void> deletePlace(@PathVariable Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new RuntimeException("장소를 찾을 수 없습니다. ID: " + placeId);
        }
        placeRepository.deleteById(placeId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
    
    /**
     * ✅ [신규] 특정 장소의 리뷰 목록을 조회합니다.
     */
    @GetMapping("/{placeId}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviewsForPlace(@PathVariable Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다. ID: " + placeId));

        if (!StringUtils.hasText(place.getReviewsJson())) {
            return ResponseEntity.ok(Collections.emptyList()); // 리뷰가 없으면 빈 리스트 반환
        }

        try {
            // reviewsJson (String)을 List<ReviewDto>로 변환
            List<ReviewDto> reviews = objectMapper.readValue(place.getReviewsJson(),
                    new TypeReference<List<ReviewDto>>() {
                    });
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            log.error("리뷰 JSON 파싱 실패 (Place ID: {})", placeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * ✅ [신규] 특정 장소의 모든 리뷰를 삭제(초기화)합니다.
     * (메인 앱에서 Google API로 다시 가져오도록 유도)
     */
    @DeleteMapping("/{placeId}/reviews")
    public ResponseEntity<Void> deleteReviewsForPlace(@PathVariable Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다. ID: " + placeId));

        // 리뷰 정보 및 캐시 상태 초기화
        place.setReviewsJson(null);
        place.setDetailsCached(false); // 👈 메인 앱이 다시 캐시하도록 설정
        placeRepository.save(place);

        return ResponseEntity.noContent().build(); // 204 No Content
    }

}
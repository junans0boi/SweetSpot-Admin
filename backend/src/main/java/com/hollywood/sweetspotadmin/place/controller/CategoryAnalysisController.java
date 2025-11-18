package com.hollywood.sweetspotadmin.place.controller;

import com.hollywood.sweetspotadmin.place.service.CategoryAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
public class CategoryAnalysisController {

    private final CategoryAnalysisService categoryAnalysisService;

    @GetMapping("/main-category-frequency")
    public ResponseEntity<Map<String, Long>> getMainCategoryFrequency() {
        Map<String, Long> frequency = categoryAnalysisService.getMainCategoryFrequency();
        return ResponseEntity.ok(frequency);
    }

    @GetMapping("/sub-category-frequency")
    public ResponseEntity<Map<String, Long>> getSubCategoryFrequency() {
        Map<String, Long> frequency = categoryAnalysisService.getSubCategoryFrequency();
        return ResponseEntity.ok(frequency);
    }

    @GetMapping("/keyword-frequency")
    public ResponseEntity<Map<String, Long>> getKeywordFrequencyInNames(
            @RequestParam(required = false) String mainCategory) {
        Map<String, Long> frequency = categoryAnalysisService.getKeywordFrequencyInNames(mainCategory);
        return ResponseEntity.ok(frequency);
    }
}

package com.hollywood.sweetspotadmin.place.service;

import com.hollywood.sweetspotadmin.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAnalysisService {

    private final PlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> getMainCategoryFrequency() {
        log.info("메인 카테고리 빈도 분석을 시작합니다.");
        return placeRepository.findMainCategoryFrequency().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getSubCategoryFrequency() {
        log.info("서브 카테고리 빈도 분석을 시작합니다.");
        return placeRepository.findSubCategoryFrequency().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getKeywordFrequencyInNames(String mainCategory) {
        log.info("{} 메인 카테고리 내 이름 키워드 빈도 분석을 시작합니다.", mainCategory != null ? mainCategory : "모든");

        Stream<String> nameStream;
        if (mainCategory != null) {
            nameStream = placeRepository.streamNamesByMainCategory(mainCategory);
        } else {
            nameStream = placeRepository.streamAllNames();
        }

        Map<String, Long> keywordFrequency = new HashMap<>();

        try (Stream<String> names = nameStream) {
            names.forEach(name -> {
                if (name == null) return;
                Arrays.stream(name.split("[\\s.,?!/()]+"))
                        .filter(word -> word.length() > 1)
                        .map(String::toLowerCase)
                        .forEach(word -> keywordFrequency.merge(word, 1L, Long::sum));
            });
        }

        return keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(200) // Return top 200 keywords to avoid huge responses
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }
}

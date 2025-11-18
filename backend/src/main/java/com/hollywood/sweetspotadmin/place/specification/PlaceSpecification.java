package com.hollywood.sweetspotadmin.place.specification;

import com.hollywood.sweetspotadmin.place.model.Place;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PlaceSpecification {

    /**
     * 동적 쿼리를 생성합니다.
     * @param mainCategory (필터)
     * @param keyword (검색어)
     * @return
     */
    public static Specification<Place> search(String mainCategory, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. mainCategory 필터 (정확히 일치)
            if (StringUtils.hasText(mainCategory)) {
                predicates.add(criteriaBuilder.equal(root.get("mainCategory"), mainCategory));
            }

            // 2. keyword 검색 (이름 OR 주소 'LIKE' 검색)
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate nameLike = criteriaBuilder.like(root.get("name"), likePattern);
                Predicate addressLike = criteriaBuilder.like(root.get("address"), likePattern);
                
                predicates.add(criteriaBuilder.or(nameLike, addressLike));
            }

            // 모든 조건을 AND로 결합
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
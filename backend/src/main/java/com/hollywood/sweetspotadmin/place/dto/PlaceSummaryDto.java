package com.hollywood.sweetspotadmin.place.dto;

import com.hollywood.sweetspotadmin.place.model.Place;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceSummaryDto {
    private Long id;
    private String name;
    private String address;
    private String mainCategory;
    private String subCategory;

    // Place 엔티티를 이 DTO로 변환하는 정적 메서드
    public static PlaceSummaryDto from(Place place) {
        return PlaceSummaryDto.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .mainCategory(place.getMainCategory())
                .subCategory(place.getSubCategory())
                .build();
    }
}
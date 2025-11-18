package com.hollywood.sweetspotadmin.place.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// JSON 파싱 시 알 수 없는 필드는 무시
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class ReviewDto {
    // Google 리뷰 JSON의 필드와 이름을 맞춥니다.
    private String author_name;
    private double rating;
    private String text;
    private long time; // (리뷰 작성 시간)
}
package com.hollywood.sweetspotadmin.place.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceUpdateRequest {
    private String name;
    private String address;
    private String mainCategory;
    private String subCategory;
}
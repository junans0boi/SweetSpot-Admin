package com.hollywood.sweetspotadmin.config;

import org.locationtech.proj4j.*; // ✅ [추가] proj4j의 모든 클래스 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proj4j 라이브러리의 CoordinateTransform 객체를 Spring Bean으로 등록합니다.
 */
@Configuration
public class Proj4jConfig {

    @Bean
    public CoordinateTransform coordinateTransform() {
        CRSFactory crsFactory = new CRSFactory();
        
        // convert.js에서 사용된 EPSG:5174 좌표계 정의
        String epsg5174Def = "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
        
        CoordinateReferenceSystem epsg5174 = crsFactory.createFromParameters("EPSG:5174", epsg5174Def);
        CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326"); // WGS84 (위도/경도)
        
        // EPSG:5174 -> WGS84 변환 객체를 생성하여 Bean으로 반환
        return new CoordinateTransformFactory().createTransform(epsg5174, wgs84);
    }
}
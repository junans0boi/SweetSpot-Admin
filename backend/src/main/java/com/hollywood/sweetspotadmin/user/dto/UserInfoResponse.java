// UserInfoResponse.java
package com.hollywood.sweetspotadmin.user.dto;

import com.hollywood.sweetspotadmin.user.model.User; // ✅ 패키지 경로 admin으로 수정
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponse {
    private Long id; // ✅ 관리용으로 ID 추가
    private String email;
    private String name;
    private String pictureUrl;
    private java.util.List<String> roles; // ✅ 역할 정보 추가

    // User 엔티티를 받아서 DTO로 변환
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(), // ✅ ID 필드
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                // ✅ Role enum을 String 리스트로 변환
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toList())
        );
    }
}
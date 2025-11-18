// RoleUpdateRequest.java
package com.hollywood.sweetspotadmin.user.dto;

import com.hollywood.sweetspotadmin.user.model.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor // (JSON 역직렬화를 위해 필요)
public class RoleUpdateRequest {
    private List<String> roles;

    // (String 리스트를 Role Enum 리스트로 변환하는 헬퍼 메서드)
    public List<Role> toRoleEnums() {
        return roles.stream()
                .map(Role::valueOf) // "ROLE_USER" -> Role.ROLE_USER
                .collect(Collectors.toList());
    }
}
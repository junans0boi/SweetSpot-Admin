package com.hollywood.sweetspotadmin.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserCreateRequest {
    private String email;
    private String name;
    private String password;
}
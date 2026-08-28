package com.mgwprod.users.dto;

import com.mgwprod.users.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String token;
    private final Long userId;
    private final String displayName;
    private final Role role;
}

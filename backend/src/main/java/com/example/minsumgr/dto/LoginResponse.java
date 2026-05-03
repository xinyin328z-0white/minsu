package com.example.minsumgr.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String token;
    private String refreshToken;

    public LoginResponse(Long userId, String username, String role, String token) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
    }

    public LoginResponse(Long userId, String username, String role, String token, String refreshToken) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
        this.refreshToken = refreshToken;
    }
}

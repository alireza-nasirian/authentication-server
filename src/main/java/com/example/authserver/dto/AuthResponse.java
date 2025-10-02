package com.example.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT tokens and user information")
public class AuthResponse {
    @Schema(description = "JWT access token for API authentication", 
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIi...")
    private String accessToken;
    
    @Schema(description = "Refresh token for obtaining new access tokens", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;
    
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";
    
    @Schema(description = "User information")
    private UserResponse user;
    
    @Schema(description = "Indicates if this is the user's first time logging in", example = "true")
    private Boolean firstTimeLogin;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public AuthResponse(String accessToken, String refreshToken, UserResponse user, Boolean firstTimeLogin) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.firstTimeLogin = firstTimeLogin;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public Boolean getFirstTimeLogin() {
        return firstTimeLogin;
    }

    public void setFirstTimeLogin(Boolean firstTimeLogin) {
        this.firstTimeLogin = firstTimeLogin;
    }
}

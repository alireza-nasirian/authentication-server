package com.example.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for Google OAuth authentication")
public class GoogleAuthRequest {
    
    @Schema(description = "Google ID token received from Android app", 
            example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjdkYzBkMjE5NjNhNmE1ODNkMjhiMGZlN...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String idToken;

    public GoogleAuthRequest() {}

    public GoogleAuthRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

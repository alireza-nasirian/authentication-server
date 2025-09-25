package com.example.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for updating user information")
public class UpdateUserRequest {
    
    @Schema(description = "User's new name (optional)", example = "John Smith", maxLength = 100)
    @Size(max = 100)
    private String name;
    
    @Schema(description = "New profile picture URL (optional)", example = "https://example.com/new-photo.jpg")
    private String profilePictureUrl;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String name, String profilePictureUrl) {
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}

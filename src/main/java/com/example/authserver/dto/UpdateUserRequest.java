package com.example.authserver.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    
    @Size(max = 100)
    private String name;
    
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

package com.example.authserver.controller;

import com.example.authserver.dto.GoogleAuthRequest;
import com.example.authserver.service.GoogleOAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    @Test
    void testGoogleAuthWithInvalidToken() throws Exception {
        // Mock invalid token response
        when(googleOAuthService.verifyGoogleToken(anyString())).thenReturn(null);

        GoogleAuthRequest request = new GoogleAuthRequest("invalid-token");

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"));
    }

    @Test
    void testGoogleAuthWithValidToken() throws Exception {
        // Mock valid token response
        GoogleOAuthService.GoogleUserInfo mockUserInfo = new GoogleOAuthService.GoogleUserInfo(
                "google123",
                "test@example.com",
                "Test User",
                "https://example.com/photo.jpg",
                "Test",
                "User"
        );
        when(googleOAuthService.verifyGoogleToken(anyString())).thenReturn(mockUserInfo);

        GoogleAuthRequest request = new GoogleAuthRequest("valid-token");

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"));
    }
}

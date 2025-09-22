package com.example.authserver.controller;

import com.example.authserver.dto.*;
import com.example.authserver.entity.AuthProvider;
import com.example.authserver.entity.RefreshToken;
import com.example.authserver.entity.User;
import com.example.authserver.security.JwtTokenProvider;
import com.example.authserver.service.GoogleOAuthService;
import com.example.authserver.service.RefreshTokenService;
import com.example.authserver.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {
        try {
            // Verify Google ID token
            GoogleOAuthService.GoogleUserInfo googleUserInfo = googleOAuthService.verifyGoogleToken(googleAuthRequest.getIdToken());
            
            if (googleUserInfo == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid Google ID token"));
            }

            // Check if user exists
            Optional<User> existingUser = userService.findByGoogleId(googleUserInfo.getGoogleId());
            User user;

            if (existingUser.isPresent()) {
                // User exists, update last login
                user = existingUser.get();
                userService.updateLastLogin(user.getId());
                
                // Update user info if changed
                boolean needsUpdate = false;
                if (!user.getName().equals(googleUserInfo.getName())) {
                    user.setName(googleUserInfo.getName());
                    needsUpdate = true;
                }
                if (!user.getProfilePictureUrl().equals(googleUserInfo.getPictureUrl())) {
                    user.setProfilePictureUrl(googleUserInfo.getPictureUrl());
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    user = userService.save(user);
                }
            } else {
                // Check if email already exists with different auth provider
                Optional<User> emailUser = userService.findByEmail(googleUserInfo.getEmail());
                if (emailUser.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Email is already registered with different authentication method"));
                }
                
                // Create new user
                user = userService.createUser(
                        googleUserInfo.getName(),
                        googleUserInfo.getEmail(),
                        googleUserInfo.getGoogleId(),
                        googleUserInfo.getPictureUrl(),
                        AuthProvider.GOOGLE
                );
            }

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), new UserResponse(user)));

        } catch (Exception ex) {
            logger.error("Error during Google authentication", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Authentication failed"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String requestRefreshToken = request.getRefreshToken();

            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
                        return ResponseEntity.ok(new TokenRefreshResponse(accessToken, requestRefreshToken));
                    })
                    .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));

        } catch (Exception ex) {
            logger.error("Error during token refresh", ex);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revokeToken(request.getRefreshToken());
            return ResponseEntity.ok(new ApiResponse(true, "Log out successful!"));
        } catch (Exception ex) {
            logger.error("Error during logout", ex);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Logout failed"));
        }
    }

    // Helper class for API responses
    public static class ApiResponse {
        private Boolean success;
        private String message;

        public ApiResponse(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // Helper class for token refresh response
    public static class TokenRefreshResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";

        public TokenRefreshResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
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
    }
}

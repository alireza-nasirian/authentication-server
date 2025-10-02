package com.example.authserver.controller;

import com.example.authserver.dto.*;
import com.example.authserver.entity.AuthProvider;
import com.example.authserver.entity.RefreshToken;
import com.example.authserver.entity.User;
import com.example.authserver.security.JwtTokenProvider;
import com.example.authserver.service.GoogleOAuthService;
import com.example.authserver.service.RefreshTokenService;
import com.example.authserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentication endpoints for Google OAuth and JWT token management")
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
    @Operation(
        summary = "Authenticate with Google OAuth",
        description = "Authenticates a user using Google ID token from Android app. Creates new user if doesn't exist, or logs in existing user. Returns firstTimeLogin flag to indicate if this is the user's first authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                        "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                        "tokenType": "Bearer",
                        "user": {
                            "id": 1,
                            "name": "John Doe",
                            "email": "john@example.com",
                            "profilePictureUrl": "https://lh3.googleusercontent.com/...",
                            "isEmailVerified": true,
                            "authProvider": "GOOGLE",
                            "createdAt": "2023-01-01T00:00:00",
                            "lastLogin": "2023-01-01T00:00:00",
                            "nameManuallyUpdated": false,
                            "profilePictureManuallyUpdated": false
                        },
                        "firstTimeLogin": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid Google ID token or email already exists with different provider",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "Invalid Google ID token"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "Authentication failed"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {
        try {
            // Verify Google ID token
            GoogleOAuthService.GoogleUserInfo googleUserInfo = googleOAuthService.verifyGoogleToken(googleAuthRequest.getIdToken());
            
            if (googleUserInfo == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse(false, "Invalid Google ID token"));
            }

            // Check if user exists
            Optional<User> existingUser = userService.findByGoogleId(googleUserInfo.getGoogleId());
            User user;
            boolean isFirstTimeLogin = false;

            if (existingUser.isPresent()) {
                // User exists, update last login
                user = existingUser.get();
                userService.updateLastLogin(user.getId());
                
                // Update user info if changed, but preserve manual updates
                boolean needsUpdate = false;
                
                // Only update name from Google if it hasn't been manually updated
                if (!user.getNameManuallyUpdated() && 
                    !user.getName().equals(googleUserInfo.getName())) {
                    user.setName(googleUserInfo.getName());
                    needsUpdate = true;
                }
                
                // Only update profile picture from Google if it hasn't been manually updated
                if (!user.getProfilePictureManuallyUpdated() && 
                    !user.getProfilePictureUrl().equals(googleUserInfo.getPictureUrl())) {
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
                            .body(new MessageResponse(false, "Email is already registered with different authentication method"));
                }
                
                // Create new user
                user = userService.createUser(
                        googleUserInfo.getName(),
                        googleUserInfo.getEmail(),
                        googleUserInfo.getGoogleId(),
                        googleUserInfo.getPictureUrl(),
                        AuthProvider.GOOGLE
                );
                isFirstTimeLogin = true;
            }

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), new UserResponse(user), isFirstTimeLogin));

        } catch (Exception ex) {
            logger.error("Error during Google authentication", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(false, "Authentication failed"));
        }
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token refresh successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenRefreshResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                        "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                        "tokenType": "Bearer"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "Refresh token was expired or revoked. Please make a new signin request"
                    }
                    """
                )
            )
        )
    })
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
                    .body(new MessageResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Revokes the refresh token to log out the user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Logout successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": true,
                        "message": "Log out successful!"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Logout failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "Logout failed"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revokeToken(request.getRefreshToken());
            return ResponseEntity.ok(new MessageResponse(true, "Log out successful!"));
        } catch (Exception ex) {
            logger.error("Error during logout", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Logout failed"));
        }
    }

    // Helper class for API responses
    public static class MessageResponse {
        private Boolean success;
        private String message;

        public MessageResponse(Boolean success, String message) {
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

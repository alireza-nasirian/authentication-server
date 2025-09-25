package com.example.authserver.controller;

import com.example.authserver.dto.UpdateUserRequest;
import com.example.authserver.dto.UserResponse;
import com.example.authserver.entity.User;
import com.example.authserver.security.UserPrincipal;
import com.example.authserver.service.RefreshTokenService;
import com.example.authserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Management", description = "CRUD operations for user accounts")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get current user information",
        description = "Retrieves the profile information of the currently authenticated user"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found"
        )
    })
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<User> user = userService.findById(userPrincipal.getId());
            
            if (user.isPresent()) {
                return ResponseEntity.ok(new UserResponse(user.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error getting current user", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Error retrieving user information"));
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user information by user ID (public endpoint)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found"
        )
    })
    public ResponseEntity<?> getUserById(
        @Parameter(description = "User ID", required = true, example = "1")
        @PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);
            
            if (user.isPresent()) {
                return ResponseEntity.ok(new UserResponse(user.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error getting user by ID: " + id, ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Error retrieving user"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> users = userService.findAll(pageable);
            
            Page<UserResponse> userResponses = users.map(UserResponse::new);
            
            return ResponseEntity.ok(userResponses);
        } catch (Exception ex) {
            logger.error("Error getting all users", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Error retrieving users"));
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Update current user",
        description = "Updates the profile information of the currently authenticated user"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    public ResponseEntity<?> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest updateUserRequest,
            Authentication authentication) {
        
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            User updatedUser = userService.updateUser(
                    userPrincipal.getId(),
                    updateUserRequest.getName(),
                    updateUserRequest.getProfilePictureUrl()
            );
            
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (Exception ex) {
            logger.error("Error updating current user", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/me/sync-with-google")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Sync profile with Google",
        description = "Resets manual update flags to allow Google profile data to overwrite local changes on next authentication"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Sync flags reset successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": true,
                        "message": "Profile will sync with Google on next login"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    public ResponseEntity<?> syncWithGoogle(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            userService.resetManualUpdateFlags(userPrincipal.getId());
            
            return ResponseEntity.ok(new MessageResponse(true, "Profile will sync with Google on next login"));
        } catch (Exception ex) {
            logger.error("Error resetting sync flags", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        
        try {
            User updatedUser = userService.updateUser(
                    id,
                    updateUserRequest.getName(),
                    updateUserRequest.getProfilePictureUrl()
            );
            
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (Exception ex) {
            logger.error("Error updating user with ID: " + id, ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, ex.getMessage()));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteCurrentUser(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<User> user = userService.findById(userPrincipal.getId());
            
            if (user.isPresent()) {
                // Revoke all refresh tokens
                refreshTokenService.deleteByUser(user.get());
                
                // Delete user
                userService.deleteUser(userPrincipal.getId());
                
                return ResponseEntity.ok(new MessageResponse(true, "User deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error deleting current user", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Error deleting user"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);
            
            if (user.isPresent()) {
                // Revoke all refresh tokens
                refreshTokenService.deleteByUser(user.get());
                
                // Delete user
                userService.deleteUser(id);
                
                return ResponseEntity.ok(new MessageResponse(true, "User deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error deleting user with ID: " + id, ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Error deleting user"));
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
}

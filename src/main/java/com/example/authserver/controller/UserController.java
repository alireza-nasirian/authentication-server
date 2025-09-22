package com.example.authserver.controller;

import com.example.authserver.dto.UpdateUserRequest;
import com.example.authserver.dto.UserResponse;
import com.example.authserver.entity.User;
import com.example.authserver.security.UserPrincipal;
import com.example.authserver.service.RefreshTokenService;
import com.example.authserver.service.UserService;
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
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
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
                    .body(new ApiResponse(false, "Error retrieving user information"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
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
                    .body(new ApiResponse(false, "Error retrieving user"));
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
                    .body(new ApiResponse(false, "Error retrieving users"));
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
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
                    .body(new ApiResponse(false, ex.getMessage()));
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
                    .body(new ApiResponse(false, ex.getMessage()));
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
                
                return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error deleting current user", ex);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error deleting user"));
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
                
                return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("Error deleting user with ID: " + id, ex);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error deleting user"));
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
}

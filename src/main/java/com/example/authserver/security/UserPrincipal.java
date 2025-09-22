package com.example.authserver.security;

import com.example.authserver.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private Long id;
    private String name;
    private String email;
    private String googleId;
    private String profilePictureUrl;
    private LocalDateTime lastLogin;

    public UserPrincipal(Long id, String name, String email, String googleId, String profilePictureUrl, LocalDateTime lastLogin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.googleId = googleId;
        this.profilePictureUrl = profilePictureUrl;
        this.lastLogin = lastLogin;
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getGoogleId(),
                user.getProfilePictureUrl(),
                user.getLastLogin()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

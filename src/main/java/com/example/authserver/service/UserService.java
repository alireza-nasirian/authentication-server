package com.example.authserver.service;

import com.example.authserver.entity.AuthProvider;
import com.example.authserver.entity.User;
import com.example.authserver.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByGoogleId(String googleId) {
        return userRepository.existsByGoogleId(googleId);
    }

    @Transactional
    public User createUser(String name, String email, String googleId, String profilePictureUrl, AuthProvider authProvider) {
        if (existsByEmail(email)) {
            throw new RuntimeException("Email is already in use!");
        }

        if (googleId != null && existsByGoogleId(googleId)) {
            throw new RuntimeException("Google ID is already in use!");
        }

        User user = new User(name, email, googleId, profilePictureUrl, authProvider);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, String name, String profilePictureUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name);
            user.setNameManuallyUpdated(true); // Mark name as manually updated
        }

        if (profilePictureUrl != null) {
            user.setProfilePictureUrl(profilePictureUrl);
            user.setProfilePictureManuallyUpdated(true); // Mark profile picture as manually updated
        }

        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(Long userId) {
        userRepository.updateLastLogin(userId, LocalDateTime.now());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User resetManualUpdateFlags(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setNameManuallyUpdated(false);
        user.setProfilePictureManuallyUpdated(false);
        
        return userRepository.save(user);
    }
}

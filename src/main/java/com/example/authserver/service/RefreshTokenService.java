package com.example.authserver.service;

import com.example.authserver.entity.RefreshToken;
import com.example.authserver.entity.User;
import com.example.authserver.repository.RefreshTokenRepository;
import com.example.authserver.repository.UserRepository;
import com.example.authserver.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${app.refreshTokenExpirationInMs:604800000}") // 7 days
    private long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              UserRepository userRepository,
                              JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke all existing refresh tokens for this user
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired() || token.getIsRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired or revoked. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeByToken(token);
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    public String generateNewAccessToken(RefreshToken refreshToken) {
        User user = refreshToken.getUser();
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
    }
}

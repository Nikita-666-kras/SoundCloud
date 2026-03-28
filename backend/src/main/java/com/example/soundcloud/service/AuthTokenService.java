package com.example.soundcloud.service;

import com.example.soundcloud.model.RefreshToken;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthTokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    public AuthTokenService(JwtService jwtService,
                            RefreshTokenRepository refreshTokenRepository,
                            UserService userService) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    @Transactional
    public TokenPair issueTokens(User user) {
        String accessToken = jwtService.buildAccessToken(user.getId());
        String refreshTokenValue = jwtService.buildRefreshToken(user.getId());
        String hash = hashToken(refreshTokenValue);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setExpiresAt(Instant.now().plusSeconds(604800)); // 7 days
        refreshTokenRepository.save(rt);
        return new TokenPair(accessToken, refreshTokenValue, jwtService.getAccessExpirationSeconds());
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}

    @Transactional
    public Optional<TokenPair> refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return Optional.empty();
        }
        if (!jwtService.isRefreshToken(refreshTokenValue)) {
            return Optional.empty();
        }
        UUID userId = jwtService.parseUserId(refreshTokenValue);
        if (userId == null) {
            return Optional.empty();
        }
        String hash = hashToken(refreshTokenValue);
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByTokenHash(hash);
        if (rtOpt.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken rt = rtOpt.get();
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(rt);
            return Optional.empty();
        }
        User user = userService.findById(rt.getUser().getId()).orElse(null);
        if (user == null) {
            refreshTokenRepository.delete(rt);
            return Optional.empty();
        }
        refreshTokenRepository.delete(rt);
        return Optional.of(issueTokens(user));
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) return;
        String hash = hashToken(refreshTokenValue);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(refreshTokenRepository::delete);
    }

    private static String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

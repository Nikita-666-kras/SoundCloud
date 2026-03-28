package com.example.soundcloud.dto;

import java.util.UUID;

public class UserDtos {

    public record RegisterRequest(String email, String username, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record LoginResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record UserResponse(UUID id, String email, String username, String bio, String avatarUrl, boolean privateAccount, boolean admin, long playCount) {
    }

    public record UpdateProfileRequest(String username, String bio, Boolean privateAccount) {
    }

    public record ArtistSummary(UUID id, String username, String bio, String avatarUrl, long playCount) {
    }
}


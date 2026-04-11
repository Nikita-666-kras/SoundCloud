package com.example.soundcloud.dto;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record StatsResponse(
            long users,
            long tracks,
            long totalPlays,
            long openReports,
            long totalReports,
            long trackLikes,
            long trackComments,
            long playlists,
            long supportMessages
    ) {
    }
}

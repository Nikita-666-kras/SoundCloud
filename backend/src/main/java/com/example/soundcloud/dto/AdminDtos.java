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
            long supportMessages,
            /** Авторизованные пользователи с активностью за последние ~5 мин (один инстанс бэкенда). */
            long onlineUsers,
            /** Средняя нагрузка ОС за 1 мин (Linux); на части сред null. */
            Double loadAverage1m,
            int availableProcessors,
            int heapUsedPercent,
            long heapUsedMb,
            long heapMaxMb,
            /** Треки с {@code createdAt} с начала календарного дня UTC. */
            long tracksUploadedToday,
            long tracksUploadedThisMonth,
            long tracksUploadedThisYear
    ) {
    }
}

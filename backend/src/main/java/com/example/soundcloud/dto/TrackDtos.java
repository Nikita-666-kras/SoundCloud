package com.example.soundcloud.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class TrackDtos {

    public record TrackUploadResponse(
            UUID id,
            String title,
            String description,
            String genre,
            String album,
            String coverUrl,
            String audioUrl,
            UUID ownerId,
            String ownerUsername,
            LocalDateTime createdAt,
            long playCount,
            long likes
    ) {
    }

    public record TrackListItem(
            UUID id,
            String title,
            String genre,
            String album,
            String coverUrl,
            UUID ownerId,
            String ownerUsername,
            long playCount,
            long likes,
            boolean likedByMe,
            LocalDateTime createdAt
    ) {
    }

    public record TrackLikeResponse(long likes, boolean likedByMe) {
    }

    public record TrackUpdateRequest(
            String title,
            String description,
            String genre,
            String album
    ) {
    }

    public record CommentResponse(
            UUID id,
            UUID userId,
            String username,
            String text,
            LocalDateTime createdAt
    ) {
    }
}


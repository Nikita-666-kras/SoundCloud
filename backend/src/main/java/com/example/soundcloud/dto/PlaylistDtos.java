package com.example.soundcloud.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PlaylistDtos {

    public record PlaylistSummary(
            UUID id,
            String name,
            String description,
            LocalDateTime createdAt,
            int trackCount
    ) {
    }

    public record PlaylistDetails(
            UUID id,
            String name,
            String description,
            LocalDateTime createdAt,
            List<TrackDtos.TrackListItem> tracks
    ) {
    }
}


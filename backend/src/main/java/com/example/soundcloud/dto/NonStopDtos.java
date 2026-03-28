package com.example.soundcloud.dto;

import java.util.UUID;

public class NonStopDtos {

    public record NonStopTrackSlot(TrackDtos.TrackListItem track, UUID promotionCampaignId) {
    }

    public record NonStopFeedbackRequest(UUID campaignId, UUID trackId, String event) {
    }

    public record CreatePromotionRequest(UUID trackId, Integer maxImpressions) {
    }

    public record PromotionMineItem(
            UUID id,
            UUID trackId,
            String trackTitle,
            int maxImpressions,
            int servedCount,
            int skipCount,
            int likeCount,
            int fullListenCount,
            boolean active
    ) {
    }
}

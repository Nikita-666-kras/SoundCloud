package com.example.soundcloud.dto;

import java.util.UUID;

public class SupportDtos {

    public record SupportMessageResponse(UUID id, String sender, String body, String createdAt) {
    }

    public record SendSupportMessageRequest(String text) {
    }

    public record AdminSupportReplyRequest(UUID userId, String text) {
    }

    public record AdminSupportThreadSummary(UUID userId, String username, String lastMessagePreview, String lastMessageAt) {
    }
}

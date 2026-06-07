package com.tempoup.api.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String content,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}

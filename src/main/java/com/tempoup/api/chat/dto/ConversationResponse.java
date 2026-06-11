package com.tempoup.api.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID matchId,
        UUID otherUserId,
        String otherDisplayName,
        OffsetDateTime lastMessageAt,
        long unreadCount
) {}

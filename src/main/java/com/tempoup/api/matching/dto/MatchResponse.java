package com.tempoup.api.matching.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchResponse(
        UUID matchId,
        UUID otherUserId,
        String otherDisplayName,
        String otherPhotoUrl,
        UUID conversationId,
        OffsetDateTime matchedAt
) {}

package com.tempoup.api.matching.dto;

import java.util.UUID;

/** Returned after a swipe; matched=true means a reciprocal LIKE created a match. */
public record SwipeResult(boolean matched, UUID matchId, UUID conversationId) {}

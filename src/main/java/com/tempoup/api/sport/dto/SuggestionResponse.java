package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.MetricType;
import com.tempoup.api.sport.SuggestionStatus;
import com.tempoup.api.sport.SuggestionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SuggestionResponse(
        UUID id,
        SuggestionType type,
        UUID parentSportId,
        String name,
        String description,
        MetricType metricType,
        SuggestionStatus status,
        String reviewNote,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt
) {}

package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.MetricType;

import java.util.UUID;

public record UserSkillResponse(
        UUID skillId,
        String name,
        MetricType metricType,
        Double weightKg,
        Integer reps,
        Double distanceKm,
        Integer durationSeconds,
        Double speedKmh,
        boolean starred
) {}

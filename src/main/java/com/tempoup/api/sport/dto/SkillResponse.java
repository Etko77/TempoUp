package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.MetricType;

import java.util.UUID;

public record SkillResponse(UUID id, UUID sportId, String name, String description, MetricType metricType) {}

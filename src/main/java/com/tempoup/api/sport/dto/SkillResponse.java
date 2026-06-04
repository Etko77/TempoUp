package com.tempoup.api.sport.dto;

import java.util.UUID;

public record SkillResponse(UUID id, UUID sportId, String name, String description) {}

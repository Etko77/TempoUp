package com.tempoup.api.sport.dto;

import java.util.UUID;

public record SportResponse(UUID id, String name, String description, String iconUrl) {}

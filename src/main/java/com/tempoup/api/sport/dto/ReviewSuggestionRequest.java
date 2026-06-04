package com.tempoup.api.sport.dto;

import jakarta.validation.constraints.Size;

public record ReviewSuggestionRequest(@Size(max = 1000) String note) {}

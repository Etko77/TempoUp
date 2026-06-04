package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.SuggestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSuggestionRequest(
        @NotNull SuggestionType type,
        /** Required when type == SKILL; ignored for SPORT. */
        UUID parentSportId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description
) {}

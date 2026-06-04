package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetUserSportRequest(
        @NotNull UUID sportId,
        @NotNull ProficiencyLevel proficiencyLevel,
        boolean priority,
        List<UUID> skillIds
) {}

package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.ProficiencyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetUserSportRequest(
        @NotNull UUID sportId,
        @NotNull ProficiencyLevel proficiencyLevel,
        boolean priority,
        @Valid List<SkillSelection> skills
) {
    public record SkillSelection(
            @NotNull UUID skillId,
            Double weightKg,
            Integer reps,
            Double distanceKm,
            Integer durationSeconds,
            Double speedKmh,
            boolean starred
    ) {}
}

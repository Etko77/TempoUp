package com.tempoup.api.sport.dto;

import com.tempoup.api.sport.ProficiencyLevel;

import java.util.List;
import java.util.UUID;

public record UserSportResponse(
        UUID sportId,
        String sportName,
        ProficiencyLevel proficiencyLevel,
        boolean priority,
        List<UserSkillResponse> skills
) {}

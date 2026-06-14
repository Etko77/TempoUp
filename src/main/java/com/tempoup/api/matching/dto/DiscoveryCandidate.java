package com.tempoup.api.matching.dto;

import java.util.List;
import java.util.UUID;

public record DiscoveryCandidate(
        UUID userId,
        String displayName,
        String bio,
        String city,
        String photoUrl,
        int sharedSports,
        int sharedSkills,
        List<String> sharedSportNames,
        int score
) {}

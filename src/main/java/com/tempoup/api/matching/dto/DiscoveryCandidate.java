package com.tempoup.api.matching.dto;

import java.util.UUID;

public record DiscoveryCandidate(
        UUID userId,
        String displayName,
        String bio,
        String city,
        String photoUrl,
        Double distanceKm,
        int sharedSports,
        int sharedSkills,
        int score
) {}

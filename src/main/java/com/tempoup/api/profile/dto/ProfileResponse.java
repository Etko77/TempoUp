package com.tempoup.api.profile.dto;

import com.tempoup.api.profile.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String displayName,
        String bio,
        LocalDate dateOfBirth,
        Gender gender,
        String photoUrl,
        String city
) {}

package com.tempoup.api.profile.dto;

import com.tempoup.api.profile.Gender;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 2000) String bio,
        LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 512) String photoUrl,
        @Size(max = 120) String city,
        Double latitude,
        Double longitude
) {}

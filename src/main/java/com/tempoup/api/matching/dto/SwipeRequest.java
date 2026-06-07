package com.tempoup.api.matching.dto;

import com.tempoup.api.matching.SwipeDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequest(
        @NotNull UUID targetUserId,
        @NotNull SwipeDirection direction
) {}

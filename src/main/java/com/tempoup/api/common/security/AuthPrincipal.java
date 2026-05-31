package com.tempoup.api.common.security;

import java.util.UUID;

/** Lightweight principal stored in the SecurityContext after JWT validation. */
public record AuthPrincipal(UUID userId, String email, String role) {}

package com.tempoup.api.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Convenience accessor for the authenticated principal. */
public final class CurrentUser {

    private CurrentUser() {}

    public static AuthPrincipal get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return p;
    }

    public static UUID id() {
        return get().userId();
    }
}

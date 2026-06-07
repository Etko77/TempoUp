package com.tempoup.api.matching.dto;

import java.util.UUID;

/**
 * Spring Data projection mapped from the native discovery query.
 * Getter names must match the column aliases (camelCase mapping).
 */
public interface DiscoveryRow {
    UUID getUserId();
    String getDisplayName();
    String getCity();
    String getBio();
    String getPhotoUrl();
    Double getDistanceKm();
    Integer getSharedSports();
    Integer getSharedPrioritySports();
    Integer getSharedSkills();
    Integer getScore();
}

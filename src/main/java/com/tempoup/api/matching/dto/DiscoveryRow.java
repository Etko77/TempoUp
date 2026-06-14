package com.tempoup.api.matching.dto;

import java.util.UUID;

public interface DiscoveryRow {
    UUID getUserId();
    String getDisplayName();
    String getCity();
    String getBio();
    String getPhotoUrl();
    Integer getSharedSports();
    Integer getSharedPrioritySports();
    Integer getSharedSkills();
    String getSharedSportNames();
    Integer getScore();
}

package com.tempoup.api.sport;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_skills")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserSkill {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "user_sport_id", nullable = false)
    private UUID userSportId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "is_starred", nullable = false)
    @Builder.Default
    private boolean starred = false;
}

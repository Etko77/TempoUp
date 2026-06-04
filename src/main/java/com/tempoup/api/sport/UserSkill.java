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
}

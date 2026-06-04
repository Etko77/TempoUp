package com.tempoup.api.sport;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sports")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserSport {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "sport_id", nullable = false)
    private UUID sportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false)
    @Builder.Default
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.BEGINNER;

    @Column(name = "is_priority", nullable = false)
    @Builder.Default
    private boolean priority = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

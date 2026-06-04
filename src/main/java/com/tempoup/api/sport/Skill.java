package com.tempoup.api.sport;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "skills")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Skill {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "sport_id", nullable = false)
    private UUID sportId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

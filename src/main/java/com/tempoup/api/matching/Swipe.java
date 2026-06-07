package com.tempoup.api.matching;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "swipes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Swipe {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "swiper_id", nullable = false)
    private UUID swiperId;

    @Column(name = "swiped_id", nullable = false)
    private UUID swipedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwipeDirection direction;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

package com.tempoup.api.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "match_id", nullable = false, unique = true)
    private UUID matchId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;
}

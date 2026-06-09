package com.tempoup.api.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

     @Modifying
     @Query("""
        UPDATE Message m 
        SET m.readAt = :readAt
        WHERE m.conversationId = :conversationId
            AND m.senderId <> :currentUserId
            AND m.readAt IS NULL
           """)
    int markConversationMessageAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("currentUserId") UUID currentUserId,
            @Param("readAt") Instant readAt
     );
}

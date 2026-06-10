package com.tempoup.api.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pushed over STOMP to a message sender when the other participant reads the
 * conversation. Lets the sender's client flip its messages to "Read · {time}".
 */
public record ReadReceipt(
        UUID conversationId,
        UUID readerId,
        OffsetDateTime readAt
) {}

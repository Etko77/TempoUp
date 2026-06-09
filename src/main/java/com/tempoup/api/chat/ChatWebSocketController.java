package com.tempoup.api.chat;

import com.tempoup.api.chat.dto.MessageResponse;
import com.tempoup.api.chat.dto.SendMessageRequest;
import com.tempoup.api.matching.Match;
import com.tempoup.api.matching.MatchRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP entry point for chat.
 *
 * Client sends to:   /app/conversations/{conversationId}/sendMessageInChat
 * Server pushes to:  /user/{otherUserId}/queue/messages   (the recipient)
 *               and: /user/{senderId}/queue/messages       (echo to sender)
 *
 * The JWT principal (user id as name) is set by StompAuthChannelInterceptor.
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chat;
    private final MatchRepository matches;
    private final SimpMessagingTemplate messaging;

    public ChatWebSocketController(ChatService chat, MatchRepository matches,
                                   SimpMessagingTemplate messaging) {
        this.chat = chat;
        this.matches = matches;
        this.messaging = messaging;
    }

    @MessageMapping("/conversations/{conversationId}/sendMessageInChat")
    public void send(@DestinationVariable UUID conversationId,
                     @Payload SendMessageRequest req,
                     Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());

        // Persist (also authorizes participant) and resolve the other party.
        MessageResponse saved = chat.sendMessage(senderId, conversationId, req);
        Conversation convo = chat.loadAndAuthorize(senderId, conversationId);
        Match match = matches.findById(convo.getMatchId()).orElseThrow();
        UUID otherId = match.getUserAId().equals(senderId) ? match.getUserBId() : match.getUserAId();

        messaging.convertAndSendToUser(otherId.toString(), "/queue/messages", saved);
        messaging.convertAndSendToUser(senderId.toString(), "/queue/messages", saved);
    }
}

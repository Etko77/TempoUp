package com.tempoup.api.chat;

import com.tempoup.api.chat.dto.ConversationResponse;
import com.tempoup.api.chat.dto.MessageResponse;
import com.tempoup.api.chat.dto.SendMessageRequest;
import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.matching.Match;
import com.tempoup.api.matching.MatchRepository;
import com.tempoup.api.profile.Profile;
import com.tempoup.api.profile.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final MatchRepository matches;
    private final ProfileRepository profiles;

    public ChatService(ConversationRepository conversations, MessageRepository messages,
                       MatchRepository matches, ProfileRepository profiles) {
        this.conversations = conversations;
        this.messages = messages;
        this.matches = matches;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UUID userId) {
        return matches.findAllForUser(userId).stream()
                .map(m -> conversations.findByMatchId(m.getId())
                        .map(c -> toConversationResponse(userId, m, c))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(UUID userId, UUID conversationId, int page, int size) {
        Conversation convo = loadAndAuthorize(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtDesc(convo.getId(), PageRequest.of(page, size))
                .map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponse sendMessage(UUID senderId, UUID conversationId, SendMessageRequest req) {
        Conversation convo = loadAndAuthorize(senderId, conversationId);
        Message saved = messages.save(Message.builder()
                .conversationId(convo.getId())
                .senderId(senderId)
                .content(req.content())
                .build());
        convo.setLastMessageAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : OffsetDateTime.now());
        conversations.save(convo);
        return toMessageResponse(saved);
    }

    /** Confirms the conversation exists and the user is one of its two participants. */
    public Conversation loadAndAuthorize(UUID userId, UUID conversationId) {
        Conversation convo = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        Match match = matches.findById(convo.getMatchId())
                .orElseThrow(() -> ApiException.notFound("Match not found"));
        if (!match.getUserAId().equals(userId) && !match.getUserBId().equals(userId)) {
            throw ApiException.forbidden("You are not a participant in this conversation");
        }
        return convo;
    }

    private ConversationResponse toConversationResponse(UUID userId, Match m, Conversation c) {
        UUID other = m.getUserAId().equals(userId) ? m.getUserBId() : m.getUserAId();
        String name = profiles.findByUserId(other).map(Profile::getDisplayName).orElse("Unknown");
        return new ConversationResponse(c.getId(), m.getId(), other, name, c.getLastMessageAt());
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getContent(), m.getReadAt(), m.getCreatedAt());
    }
}

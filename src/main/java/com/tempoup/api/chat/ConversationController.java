package com.tempoup.api.chat;

import com.tempoup.api.chat.dto.ConversationResponse;
import com.tempoup.api.chat.dto.MessageResponse;
import com.tempoup.api.chat.dto.SendMessageRequest;
import com.tempoup.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chat;

    public ConversationController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public List<ConversationResponse> myConversations() {
        return chat.listConversations(CurrentUser.id());
    }

    @GetMapping("/{conversationId}/messages")
    public Page<MessageResponse> messages(@PathVariable UUID conversationId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "30") int size) {
        return chat.listMessages(CurrentUser.id(), conversationId, page, size);
    }

    @PostMapping("/{conversationId}/messages")
    public MessageResponse send(@PathVariable UUID conversationId,
                                @Valid @RequestBody SendMessageRequest req) {
        return chat.sendMessage(CurrentUser.id(), conversationId, req);
    }
}

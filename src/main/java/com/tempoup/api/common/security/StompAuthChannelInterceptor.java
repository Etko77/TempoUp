package com.tempoup.api.common.security;

import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Reads the "Authorization: Bearer <jwt>" header on the STOMP CONNECT frame and
 * attaches an authenticated principal to the WebSocket session. The principal's
 * name is the user id, which lets us route to /user/{id}/queue/... destinations.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> auth = accessor.getNativeHeader("Authorization");
            if (auth != null && !auth.isEmpty() && auth.get(0).startsWith("Bearer ")) {
                String token = auth.get(0).substring(7);
                Claims claims = jwtService.parse(token); // throws if invalid
                UUID userId = UUID.fromString(claims.getSubject());
                String role = claims.get("role", String.class);
                var principal = new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                accessor.setUser(principal);
            }
        }
        return message;
    }
}

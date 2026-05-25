package com.tempoup.api.auth;

import com.tempoup.api.auth.dto.*;
import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.common.security.JwtService;
import com.tempoup.api.config.AppProperties;
import com.tempoup.api.profile.Profile;
import com.tempoup.api.profile.ProfileRepository;
import com.tempoup.api.user.Role;
import com.tempoup.api.user.User;
import com.tempoup.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, ProfileRepository profiles,
                       RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, AppProperties props) {
        this.users = users;
        this.profiles = profiles;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already registered");
        }
        User user = users.save(User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.USER)
                .enabled(true)
                .build());

        profiles.save(Profile.builder()
                .userId(user.getId())
                .displayName(req.displayName())
                .build());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Account disabled");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String hash = sha256(req.refreshToken());
        RefreshToken stored = refreshTokens.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.unauthorized("Refresh token expired");
        }
        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        // Rotate: revoke old, issue new.
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        refreshTokens.revokeAllForUser(userId);
    }

    private AuthResponse issueTokens(User user) {
        String access = jwt.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshRaw = generateOpaqueToken();
        refreshTokens.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(refreshRaw))
                .expiresAt(OffsetDateTime.now().plusDays(props.jwt().refreshTokenTtlDays()))
                .revoked(false)
                .build());
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), access, refreshRaw);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

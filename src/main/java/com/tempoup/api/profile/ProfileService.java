package com.tempoup.api.profile;

import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.profile.dto.ProfileResponse;
import com.tempoup.api.profile.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByUserId(UUID userId) {
        return toResponse(load(userId));
    }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest req) {
        Profile p = load(userId);
        if (req.displayName() != null) p.setDisplayName(req.displayName());
        if (req.bio() != null)         p.setBio(req.bio());
        if (req.dateOfBirth() != null) p.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null)      p.setGender(req.gender());
        if (req.photoUrl() != null)    p.setPhotoUrl(req.photoUrl());
        if (req.city() != null)        p.setCity(req.city());
        return toResponse(profiles.save(p));
    }

    private Profile load(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
    }

    private ProfileResponse toResponse(Profile p) {
        return new ProfileResponse(
                p.getUserId(), p.getDisplayName(), p.getBio(), p.getDateOfBirth(),
                p.getGender(), p.getPhotoUrl(), p.getCity());
    }
}

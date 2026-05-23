package com.tempoup.api.profile;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.profile.dto.ProfileResponse;
import com.tempoup.api.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public ProfileResponse myProfile() {
        return profiles.getByUserId(CurrentUser.id());
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return profiles.update(CurrentUser.id(), req);
    }

    @GetMapping("/{userId}")
    public ProfileResponse byUser(@PathVariable UUID userId) {
        return profiles.getByUserId(userId);
    }
}

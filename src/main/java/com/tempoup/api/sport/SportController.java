package com.tempoup.api.sport;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.sport.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sports")
public class SportController {

    private final SportService sportService;
    private final UserSportService userSports;
    private final SuggestionService suggestions;

    public SportController(SportService sportService, UserSportService userSports,
                           SuggestionService suggestions) {
        this.sportService = sportService;
        this.userSports = userSports;
        this.suggestions = suggestions;
    }

    @GetMapping
    public List<SportResponse> listSports() {
        return sportService.listSports();
    }

    @GetMapping("/{sportId}/skills")
    public List<SkillResponse> listSkills(@PathVariable UUID sportId) {
        return sportService.listSkills(sportId);
    }

    @GetMapping("/mine")
    public List<UserSportResponse> mySports() {
        return userSports.listForUser(CurrentUser.id());
    }

    @PutMapping("/mine")
    public UserSportResponse setMySport(@Valid @RequestBody SetUserSportRequest req) {
        return userSports.setUserSport(CurrentUser.id(), req);
    }

    @DeleteMapping("/mine/{sportId}")
    public ResponseEntity<Void> removeMySport(@PathVariable UUID sportId) {
        userSports.removeUserSport(CurrentUser.id(), sportId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-user/{userId}")
    public List<UserSportResponse> sportsByUser(@PathVariable UUID userId) {
        return userSports.listForUser(userId);
    }

    @PostMapping("/suggestions")
    public ResponseEntity<SuggestionResponse> suggest(@Valid @RequestBody CreateSuggestionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(suggestions.create(CurrentUser.id(), req));
    }

    @GetMapping("/suggestions/mine")
    public List<SuggestionResponse> mySuggestions() {
        return suggestions.listMine(CurrentUser.id());
    }
}

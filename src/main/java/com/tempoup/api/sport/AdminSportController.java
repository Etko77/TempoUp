package com.tempoup.api.sport;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.sport.dto.ReviewSuggestionRequest;
import com.tempoup.api.sport.dto.SuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/suggestions")
public class AdminSportController {

    private final SuggestionService suggestions;

    public AdminSportController(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @GetMapping
    public List<SuggestionResponse> pending() {
        return suggestions.listPending();
    }

    @PostMapping("/{id}/approve")
    public SuggestionResponse approve(@PathVariable UUID id,
                                      @Valid @RequestBody(required = false) ReviewSuggestionRequest req) {
        return suggestions.approve(CurrentUser.id(), id, req);
    }

    @PostMapping("/{id}/reject")
    public SuggestionResponse reject(@PathVariable UUID id,
                                     @Valid @RequestBody(required = false) ReviewSuggestionRequest req) {
        return suggestions.reject(CurrentUser.id(), id, req);
    }
}

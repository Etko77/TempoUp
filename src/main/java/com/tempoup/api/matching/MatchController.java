package com.tempoup.api.matching;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.matching.dto.MatchResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchingService matching;

    public MatchController(MatchingService matching) {
        this.matching = matching;
    }

    @GetMapping
    public List<MatchResponse> myMatches() {
        return matching.listMatches(CurrentUser.id());
    }
}

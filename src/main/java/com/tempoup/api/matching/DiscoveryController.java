package com.tempoup.api.matching;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.matching.dto.DiscoveryCandidate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final MatchingService matching;

    public DiscoveryController(MatchingService matching) {
        this.matching = matching;
    }

    @GetMapping
    public List<DiscoveryCandidate> feed(@RequestParam(required = false) Integer limit) {
        return matching.feed(CurrentUser.id(), limit);
    }

    @GetMapping("/by-sport/{sportId}")
    public List<DiscoveryCandidate> feedBySport(@PathVariable UUID sportId,
                                                @RequestParam(required = false) Integer limit) {
        return matching.feedBySport(CurrentUser.id(), sportId, limit);
    }
}

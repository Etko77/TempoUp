package com.tempoup.api.matching;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.matching.dto.DiscoveryCandidate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final MatchingService matching;

    public DiscoveryController(MatchingService matching) {
        this.matching = matching;
    }

    /** The swipe feed, ordered by match score then proximity. */
    @GetMapping
    public List<DiscoveryCandidate> feed(@RequestParam(required = false) Double radiusKm,
                                         @RequestParam(required = false) Integer limit) {
        return matching.feed(CurrentUser.id(), radiusKm, limit);
    }
}

package com.tempoup.api.matching;

import com.tempoup.api.common.security.CurrentUser;
import com.tempoup.api.matching.dto.SwipeRequest;
import com.tempoup.api.matching.dto.SwipeResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/swipes")
public class SwipeController {

    private final MatchingService matching;

    public SwipeController(MatchingService matching) {
        this.matching = matching;
    }

    @PostMapping
    public SwipeResult swipe(@Valid @RequestBody SwipeRequest req) {
        return matching.swipe(CurrentUser.id(), req);
    }
}

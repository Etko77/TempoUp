package com.tempoup.api.matching;

import com.tempoup.api.chat.Conversation;
import com.tempoup.api.chat.ConversationRepository;
import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.config.AppProperties;
import com.tempoup.api.matching.dto.*;
import com.tempoup.api.profile.Profile;
import com.tempoup.api.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchingService {

    private final DiscoveryRepository discovery;
    private final SwipeRepository swipes;
    private final MatchRepository matches;
    private final ConversationRepository conversations;
    private final ProfileRepository profiles;
    private final AppProperties props;

    public MatchingService(DiscoveryRepository discovery, SwipeRepository swipes,
                           MatchRepository matches, ConversationRepository conversations,
                           ProfileRepository profiles, AppProperties props) {
        this.discovery = discovery;
        this.swipes = swipes;
        this.matches = matches;
        this.conversations = conversations;
        this.profiles = profiles;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<DiscoveryCandidate> feed(UUID userId, Double radiusKm, Integer limit) {
        double radiusMeters = (radiusKm != null ? radiusKm : props.matching().defaultRadiusKm()) * 1000.0;
        int max = limit != null ? Math.min(limit, props.matching().maxFeedSize()) : props.matching().maxFeedSize();
        return discovery.findFeed(userId, radiusMeters, max).stream()
                .map(r -> new DiscoveryCandidate(
                        r.getUserId(), r.getDisplayName(), r.getBio(), r.getCity(), r.getPhotoUrl(),
                        r.getDistanceKm(),
                        r.getSharedSports() == null ? 0 : r.getSharedSports(),
                        r.getSharedSkills() == null ? 0 : r.getSharedSkills(),
                        r.getScore() == null ? 0 : r.getScore()))
                .toList();
    }

    /**
     * Record a swipe. If it is a LIKE and the target already liked the swiper,
     * a Match (and its Conversation) is created atomically.
     */
    @Transactional
    public SwipeResult swipe(UUID swiperId, SwipeRequest req) {
        UUID targetId = req.targetUserId();
        if (swiperId.equals(targetId)) {
            throw ApiException.badRequest("You cannot swipe on yourself");
        }
        if (profiles.findByUserId(targetId).isEmpty()) {
            throw ApiException.notFound("Target user not found");
        }
        if (swipes.existsBySwiperIdAndSwipedId(swiperId, targetId)) {
            throw ApiException.conflict("You have already swiped on this user");
        }

        swipes.save(Swipe.builder()
                .swiperId(swiperId)
                .swipedId(targetId)
                .direction(req.direction())
                .build());

        if (req.direction() == SwipeDirection.LIKE
                && swipes.existsBySwiperIdAndSwipedIdAndDirection(targetId, swiperId, SwipeDirection.LIKE)) {
            Match match = matches.save(Match.ordered(swiperId, targetId));
            Conversation convo = conversations.save(Conversation.builder()
                    .matchId(match.getId())
                    .build());
            return new SwipeResult(true, match.getId(), convo.getId());
        }
        return new SwipeResult(false, null, null);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listMatches(UUID userId) {
        return matches.findAllForUser(userId).stream().map(m -> {
            UUID other = m.getUserAId().equals(userId) ? m.getUserBId() : m.getUserAId();
            Optional<Profile> op = profiles.findByUserId(other);
            UUID convoId = conversations.findByMatchId(m.getId())
                    .map(Conversation::getId).orElse(null);
            return new MatchResponse(
                    m.getId(), other,
                    op.map(Profile::getDisplayName).orElse("Unknown"),
                    op.map(Profile::getPhotoUrl).orElse(null),
                    convoId, m.getMatchedAt());
        }).toList();
    }
}

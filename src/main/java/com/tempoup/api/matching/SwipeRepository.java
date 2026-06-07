package com.tempoup.api.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    Optional<Swipe> findBySwiperIdAndSwipedId(UUID swiperId, UUID swipedId);

    boolean existsBySwiperIdAndSwipedId(UUID swiperId, UUID swipedId);

    /** Did the other user already LIKE me? Used to detect a reciprocal match. */
    boolean existsBySwiperIdAndSwipedIdAndDirection(UUID swiperId, UUID swipedId, SwipeDirection direction);
}

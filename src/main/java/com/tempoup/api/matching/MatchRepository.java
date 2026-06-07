package com.tempoup.api.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("select m from Match m where m.userAId = :userId or m.userBId = :userId order by m.matchedAt desc")
    List<Match> findAllForUser(@Param("userId") UUID userId);
}

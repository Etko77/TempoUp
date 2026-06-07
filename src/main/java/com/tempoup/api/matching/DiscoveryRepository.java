package com.tempoup.api.matching;

import com.tempoup.api.matching.dto.DiscoveryRow;
import com.tempoup.api.profile.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for building the swipe feed.
 *
 * The scoring logic, expressed in SQL:
 *   - candidates are users (with a profile) that the current user has NOT swiped on
 *   - exclude self and disabled users
 *   - within :radiusMeters of the current user's location (if both have a location)
 *   - score = (count of shared sports) * 10
 *           + (count of shared sports flagged priority by EITHER user) * 5
 *           + (count of shared skills) * 2
 *   - order by score desc, then distance asc
 *
 * Returns a lightweight projection (DiscoveryRow) rather than full entities.
 */
public interface DiscoveryRepository extends Repository<Profile, UUID> {

    @Query(value = """
        WITH me AS (
            SELECT p.user_id, p.location
            FROM profiles p
            WHERE p.user_id = :userId
        ),
        my_sports AS (
            SELECT us.sport_id, bool_or(us.is_priority) AS my_priority
            FROM user_sports us
            WHERE us.user_id = :userId
            GROUP BY us.sport_id
        ),
        my_skills AS (
            SELECT uk.skill_id
            FROM user_skills uk
            JOIN user_sports us ON us.id = uk.user_sport_id
            WHERE us.user_id = :userId
        )
        SELECT
            cand.user_id                                              AS user_id,
            cand.display_name                                         AS display_name,
            cand.city                                                 AS city,
            cand.bio                                                  AS bio,
            cand.photo_url                                            AS photo_url,
            CASE
                WHEN me.location IS NOT NULL AND cand.location IS NOT NULL
                THEN ST_Distance(me.location, cand.location) / 1000.0
                ELSE NULL
            END                                                       AS distance_km,
            COALESCE(shared.shared_sports, 0)                         AS shared_sports,
            COALESCE(shared.shared_priority_sports, 0)                AS shared_priority_sports,
            COALESCE(sk.shared_skills, 0)                             AS shared_skills,
            (COALESCE(shared.shared_sports, 0) * 10
             + COALESCE(shared.shared_priority_sports, 0) * 5
             + COALESCE(sk.shared_skills, 0) * 2)                     AS score
        FROM profiles cand
        JOIN users u ON u.id = cand.user_id AND u.enabled = TRUE
        CROSS JOIN me
        LEFT JOIN (
            SELECT us.user_id,
                   COUNT(*) AS shared_sports,
                   COUNT(*) FILTER (WHERE us.is_priority OR ms.my_priority) AS shared_priority_sports
            FROM user_sports us
            JOIN my_sports ms ON ms.sport_id = us.sport_id
            GROUP BY us.user_id
        ) shared ON shared.user_id = cand.user_id
        LEFT JOIN (
            SELECT us.user_id, COUNT(DISTINCT uk.skill_id) AS shared_skills
            FROM user_skills uk
            JOIN user_sports us ON us.id = uk.user_sport_id
            JOIN my_skills msk ON msk.skill_id = uk.skill_id
            GROUP BY us.user_id
        ) sk ON sk.user_id = cand.user_id
        WHERE cand.user_id <> :userId
          AND NOT EXISTS (
              SELECT 1 FROM swipes s
              WHERE s.swiper_id = :userId AND s.swiped_id = cand.user_id
          )
          AND (
              me.location IS NULL
              OR cand.location IS NULL
              OR ST_DWithin(me.location, cand.location, :radiusMeters)
          )
        ORDER BY score DESC, distance_km ASC NULLS LAST, cand.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DiscoveryRow> findFeed(@Param("userId") UUID userId,
                                @Param("radiusMeters") double radiusMeters,
                                @Param("limit") int limit);
}

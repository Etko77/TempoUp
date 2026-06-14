package com.tempoup.api.matching;

import com.tempoup.api.matching.dto.DiscoveryRow;
import com.tempoup.api.profile.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DiscoveryRepository extends Repository<Profile, UUID> {

    @Query(value = """
        WITH my_sports AS (
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
            COALESCE(shared.shared_sports, 0)                         AS shared_sports,
            COALESCE(shared.shared_priority_sports, 0)                AS shared_priority_sports,
            COALESCE(sk.shared_skills, 0)                             AS shared_skills,
            shared.shared_sport_names                                 AS shared_sport_names,
            (COALESCE(shared.shared_sports, 0) * 10
             + COALESCE(shared.shared_priority_sports, 0) * 5
             + COALESCE(sk.shared_skills, 0) * 2)                     AS score
        FROM profiles cand
        JOIN users u ON u.id = cand.user_id AND u.enabled = TRUE
        LEFT JOIN (
            SELECT us.user_id,
                   COUNT(*) AS shared_sports,
                   COUNT(*) FILTER (WHERE us.is_priority OR ms.my_priority) AS shared_priority_sports,
                   string_agg(DISTINCT sp.name, ', ' ORDER BY sp.name) AS shared_sport_names
            FROM user_sports us
            JOIN my_sports ms ON ms.sport_id = us.sport_id
            JOIN sports sp ON sp.id = us.sport_id
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
                AND (s.direction = 'LIKE' OR s.created_at > now() - interval '1 hour')
          )
        ORDER BY shared_sports DESC, shared_skills DESC, cand.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DiscoveryRow> findFeed(@Param("userId") UUID userId,
                                @Param("limit") int limit);

    @Query(value = """
        WITH my_sports AS (
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
            COALESCE(shared.shared_sports, 0)                         AS shared_sports,
            COALESCE(shared.shared_priority_sports, 0)                AS shared_priority_sports,
            COALESCE(sk.shared_skills, 0)                             AS shared_skills,
            shared.shared_sport_names                                 AS shared_sport_names,
            (COALESCE(sk.shared_skills, 0) * 10
             + COALESCE(shared.shared_sports, 0))                     AS score
        FROM profiles cand
        JOIN users u ON u.id = cand.user_id AND u.enabled = TRUE
        JOIN user_sports cand_sport
             ON cand_sport.user_id = cand.user_id AND cand_sport.sport_id = :sportId
        LEFT JOIN (
            SELECT us.user_id,
                   COUNT(*) AS shared_sports,
                   COUNT(*) FILTER (WHERE us.is_priority OR ms.my_priority) AS shared_priority_sports,
                   string_agg(DISTINCT sp.name, ', ' ORDER BY sp.name) AS shared_sport_names
            FROM user_sports us
            JOIN my_sports ms ON ms.sport_id = us.sport_id
            JOIN sports sp ON sp.id = us.sport_id
            GROUP BY us.user_id
        ) shared ON shared.user_id = cand.user_id
        LEFT JOIN (
            SELECT us.user_id, COUNT(DISTINCT uk.skill_id) AS shared_skills
            FROM user_skills uk
            JOIN user_sports us ON us.id = uk.user_sport_id
            JOIN my_skills msk ON msk.skill_id = uk.skill_id
            JOIN skills s ON s.id = uk.skill_id AND s.sport_id = :sportId
            GROUP BY us.user_id
        ) sk ON sk.user_id = cand.user_id
        WHERE cand.user_id <> :userId
          AND NOT EXISTS (
              SELECT 1 FROM swipes s
              WHERE s.swiper_id = :userId AND s.swiped_id = cand.user_id
                AND (s.direction = 'LIKE' OR s.created_at > now() - interval '1 hour')
          )
        ORDER BY shared_skills DESC, shared_sports DESC, cand.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DiscoveryRow> findFeedBySport(@Param("userId") UUID userId,
                                       @Param("sportId") UUID sportId,
                                       @Param("limit") int limit);
}

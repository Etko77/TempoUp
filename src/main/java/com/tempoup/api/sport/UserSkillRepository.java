package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {
    List<UserSkill> findByUserSportId(UUID userSportId);

    @Query("""
        select count(uk) from UserSkill uk, UserSport us
        where us.id = uk.userSportId
          and us.userId = :userId
          and uk.starred = true
          and us.id <> :excludeUserSportId
        """)
    long countStarredForUserExcluding(@Param("userId") UUID userId,
                                      @Param("excludeUserSportId") UUID excludeUserSportId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserSkill u where u.userSportId = :userSportId")
    void deleteByUserSportId(@Param("userSportId") UUID userSportId);
}

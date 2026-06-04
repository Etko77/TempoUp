package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {
    List<UserSkill> findByUserSportId(UUID userSportId);
    void deleteByUserSportId(UUID userSportId);
}

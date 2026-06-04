package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findBySportIdAndActiveTrueOrderByName(UUID sportId);
    boolean existsBySportIdAndNameIgnoreCase(UUID sportId, String name);
}

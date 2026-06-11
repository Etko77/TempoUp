package com.tempoup.api.sport;

import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.sport.dto.SetUserSportRequest;
import com.tempoup.api.sport.dto.SetUserSportRequest.SkillSelection;
import com.tempoup.api.sport.dto.UserSkillResponse;
import com.tempoup.api.sport.dto.UserSportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserSportService {

    private static final int MAX_STARRED_SKILLS = 3;

    private final UserSportRepository userSports;
    private final UserSkillRepository userSkills;
    private final SportRepository sports;
    private final SkillRepository skills;

    public UserSportService(UserSportRepository userSports, UserSkillRepository userSkills,
                            SportRepository sports, SkillRepository skills) {
        this.userSports = userSports;
        this.userSkills = userSkills;
        this.sports = sports;
        this.skills = skills;
    }

    @Transactional
    public UserSportResponse setUserSport(UUID userId, SetUserSportRequest req) {
        Sport sport = sports.findById(req.sportId())
                .orElseThrow(() -> ApiException.notFound("Sport not found"));

        UserSport us = userSports.findByUserIdAndSportId(userId, req.sportId())
                .orElseGet(() -> UserSport.builder()
                        .userId(userId)
                        .sportId(req.sportId())
                        .build());
        us.setProficiencyLevel(req.proficiencyLevel());
        us.setPriority(req.priority());
        us = userSports.save(us);

        List<SkillSelection> selections = req.skills() != null ? req.skills() : List.of();

        long starredHere = selections.stream().filter(SkillSelection::starred).count();
        long starredElsewhere = userSkills.countStarredForUserExcluding(userId, us.getId());
        if (starredHere + starredElsewhere > MAX_STARRED_SKILLS) {
            throw ApiException.badRequest(
                    "You can star at most " + MAX_STARRED_SKILLS + " skills across your profile");
        }

        userSkills.deleteByUserSportId(us.getId());
        for (SkillSelection sel : selections) {
            Skill skill = skills.findById(sel.skillId())
                    .orElseThrow(() -> ApiException.notFound("Skill not found: " + sel.skillId()));
            if (!skill.getSportId().equals(req.sportId())) {
                throw ApiException.badRequest("Skill " + sel.skillId() + " does not belong to the sport");
            }
            userSkills.save(UserSkill.builder()
                    .userSportId(us.getId())
                    .skillId(sel.skillId())
                    .weightKg(sel.weightKg())
                    .reps(sel.reps())
                    .distanceKm(sel.distanceKm())
                    .durationSeconds(sel.durationSeconds())
                    .speedKmh(sel.speedKmh())
                    .starred(sel.starred())
                    .build());
        }
        return toResponse(sport, us);
    }

    @Transactional(readOnly = true)
    public List<UserSportResponse> listForUser(UUID userId) {
        return userSports.findByUserId(userId).stream().map(us -> {
            Sport sport = sports.findById(us.getSportId())
                    .orElseThrow(() -> ApiException.notFound("Sport not found"));
            return toResponse(sport, us);
        }).toList();
    }

    @Transactional
    public void removeUserSport(UUID userId, UUID sportId) {
        UserSport us = userSports.findByUserIdAndSportId(userId, sportId)
                .orElseThrow(() -> ApiException.notFound("You have not selected this sport"));
        userSkills.deleteByUserSportId(us.getId());
        userSports.delete(us);
    }

    private UserSportResponse toResponse(Sport sport, UserSport us) {
        List<UserSkillResponse> skillDtos = userSkills.findByUserSportId(us.getId()).stream()
                .map(uk -> {
                    Skill sk = skills.findById(uk.getSkillId()).orElse(null);
                    if (sk == null) return null;
                    return new UserSkillResponse(
                            sk.getId(), sk.getName(), sk.getMetricType(),
                            uk.getWeightKg(), uk.getReps(), uk.getDistanceKm(),
                            uk.getDurationSeconds(), uk.getSpeedKmh(), uk.isStarred());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new UserSportResponse(
                sport.getId(), sport.getName(),
                us.getProficiencyLevel(), us.isPriority(), skillDtos);
    }
}

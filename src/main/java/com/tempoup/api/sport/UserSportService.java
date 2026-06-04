package com.tempoup.api.sport;

import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.sport.dto.SetUserSportRequest;
import com.tempoup.api.sport.dto.SkillResponse;
import com.tempoup.api.sport.dto.UserSportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the sports/skills a given user has selected for their profile.
 * Selecting a sport upserts the user_sports row and replaces its skill set.
 */
@Service
public class UserSportService {

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

        // Replace skills: clear existing, then add the requested ones (validated).
        userSkills.deleteByUserSportId(us.getId());
        if (req.skillIds() != null) {
            for (UUID skillId : req.skillIds()) {
                Skill skill = skills.findById(skillId)
                        .orElseThrow(() -> ApiException.notFound("Skill not found: " + skillId));
                if (!skill.getSportId().equals(req.sportId())) {
                    throw ApiException.badRequest("Skill " + skillId + " does not belong to the sport");
                }
                userSkills.save(UserSkill.builder()
                        .userSportId(us.getId())
                        .skillId(skillId)
                        .build());
            }
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
        List<SkillResponse> skillDtos = userSkills.findByUserSportId(us.getId()).stream()
                .map(uk -> skills.findById(uk.getSkillId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(sk -> new SkillResponse(sk.getId(), sk.getSportId(), sk.getName(), sk.getDescription()))
                .toList();
        return new UserSportResponse(
                sport.getId(), sport.getName(),
                us.getProficiencyLevel(), us.isPriority(), skillDtos);
    }
}

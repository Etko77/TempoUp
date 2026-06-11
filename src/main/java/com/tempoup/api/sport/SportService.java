package com.tempoup.api.sport;

import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.sport.dto.SkillResponse;
import com.tempoup.api.sport.dto.SportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SportService {

    private final SportRepository sports;
    private final SkillRepository skills;

    public SportService(SportRepository sports, SkillRepository skills) {
        this.sports = sports;
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public List<SportResponse> listSports() {
        return sports.findByActiveTrueOrderByName().stream()
                .map(s -> new SportResponse(s.getId(), s.getName(), s.getDescription(), s.getIconUrl()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills(UUID sportId) {
        if (!sports.existsById(sportId)) {
            throw ApiException.notFound("Sport not found");
        }
        return skills.findBySportIdAndActiveTrueOrderByName(sportId).stream()
                .map(sk -> new SkillResponse(sk.getId(), sk.getSportId(), sk.getName(), sk.getDescription(), sk.getMetricType()))
                .toList();
    }
}

package com.tempoup.api.sport;

import com.tempoup.api.common.exception.ApiException;
import com.tempoup.api.sport.dto.CreateSuggestionRequest;
import com.tempoup.api.sport.dto.ReviewSuggestionRequest;
import com.tempoup.api.sport.dto.SuggestionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SuggestionService {

    private final SportSuggestionRepository suggestions;
    private final SportRepository sports;
    private final SkillRepository skills;

    public SuggestionService(SportSuggestionRepository suggestions,
                             SportRepository sports, SkillRepository skills) {
        this.suggestions = suggestions;
        this.sports = sports;
        this.skills = skills;
    }

    @Transactional
    public SuggestionResponse create(UUID userId, CreateSuggestionRequest req) {
        if (req.type() == SuggestionType.SKILL) {
            if (req.parentSportId() == null) {
                throw ApiException.badRequest("parentSportId is required for a skill suggestion");
            }
            if (!sports.existsById(req.parentSportId())) {
                throw ApiException.notFound("Parent sport not found");
            }
        }
        SportSuggestion s = suggestions.save(SportSuggestion.builder()
                .type(req.type())
                .parentSportId(req.type() == SuggestionType.SKILL ? req.parentSportId() : null)
                .name(req.name().trim())
                .description(req.description())
                .metricType(req.type() == SuggestionType.SKILL
                        ? (req.metricType() != null ? req.metricType() : MetricType.NONE)
                        : null)
                .status(SuggestionStatus.PENDING)
                .suggestedBy(userId)
                .build());
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> listMine(UUID userId) {
        return suggestions.findBySuggestedByOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> listPending() {
        return suggestions.findByStatusOrderByCreatedAtAsc(SuggestionStatus.PENDING).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public SuggestionResponse approve(UUID adminId, UUID suggestionId, ReviewSuggestionRequest req) {
        SportSuggestion s = load(suggestionId);
        requirePending(s);

        if (s.getType() == SuggestionType.SPORT) {
            if (sports.existsByNameIgnoreCase(s.getName())) {
                throw ApiException.conflict("A sport with this name already exists");
            }
            sports.save(Sport.builder()
                    .name(s.getName())
                    .description(s.getDescription())
                    .active(true)
                    .createdBy(s.getSuggestedBy())
                    .build());
        } else {
            UUID sportId = s.getParentSportId();
            if (sportId == null || !sports.existsById(sportId)) {
                throw ApiException.badRequest("Parent sport no longer exists");
            }
            if (skills.existsBySportIdAndNameIgnoreCase(sportId, s.getName())) {
                throw ApiException.conflict("This skill already exists for the sport");
            }
            skills.save(Skill.builder()
                    .sportId(sportId)
                    .name(s.getName())
                    .description(s.getDescription())
                    .metricType(s.getMetricType() != null ? s.getMetricType() : MetricType.NONE)
                    .active(true)
                    .createdBy(s.getSuggestedBy())
                    .build());
        }

        s.setStatus(SuggestionStatus.APPROVED);
        s.setReviewedBy(adminId);
        s.setReviewNote(req != null ? req.note() : null);
        s.setReviewedAt(OffsetDateTime.now());
        return toResponse(suggestions.save(s));
    }

    @Transactional
    public SuggestionResponse reject(UUID adminId, UUID suggestionId, ReviewSuggestionRequest req) {
        SportSuggestion s = load(suggestionId);
        requirePending(s);
        s.setStatus(SuggestionStatus.REJECTED);
        s.setReviewedBy(adminId);
        s.setReviewNote(req != null ? req.note() : null);
        s.setReviewedAt(OffsetDateTime.now());
        return toResponse(suggestions.save(s));
    }

    private void requirePending(SportSuggestion s) {
        if (s.getStatus() != SuggestionStatus.PENDING) {
            throw ApiException.conflict("Suggestion has already been reviewed");
        }
    }

    private SportSuggestion load(UUID id) {
        return suggestions.findById(id)
                .orElseThrow(() -> ApiException.notFound("Suggestion not found"));
    }

    private SuggestionResponse toResponse(SportSuggestion s) {
        return new SuggestionResponse(s.getId(), s.getType(), s.getParentSportId(),
                s.getName(), s.getDescription(), s.getMetricType(), s.getStatus(), s.getReviewNote(),
                s.getCreatedAt(), s.getReviewedAt());
    }
}

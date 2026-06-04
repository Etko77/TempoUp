package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SportSuggestionRepository extends JpaRepository<SportSuggestion, UUID> {
    List<SportSuggestion> findByStatusOrderByCreatedAtAsc(SuggestionStatus status);
    List<SportSuggestion> findBySuggestedByOrderByCreatedAtDesc(UUID suggestedBy);
}

package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSportRepository extends JpaRepository<UserSport, UUID> {
    List<UserSport> findByUserId(UUID userId);
    Optional<UserSport> findByUserIdAndSportId(UUID userId, UUID sportId);
    void deleteByUserIdAndSportId(UUID userId, UUID sportId);
}

package com.tempoup.api.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportRepository extends JpaRepository<Sport, UUID> {
    List<Sport> findByActiveTrueOrderByName();
    Optional<Sport> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

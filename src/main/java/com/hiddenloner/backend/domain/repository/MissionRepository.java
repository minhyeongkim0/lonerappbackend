package com.hiddenloner.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.Mission;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByIsActiveTrueOrderByCreatedAtDesc();
}

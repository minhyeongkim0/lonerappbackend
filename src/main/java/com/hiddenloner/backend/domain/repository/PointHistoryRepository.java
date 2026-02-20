package com.hiddenloner.backend.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.PointHistory;

public interface PointHistoryRepository extends JpaRepository<PointHistory, UUID> {
}

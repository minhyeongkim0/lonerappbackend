package com.hiddenloner.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.RecommendationPost;

public interface RecommendationPostRepository extends JpaRepository<RecommendationPost, UUID> {
    List<RecommendationPost> findAllByOrderByCreatedAtDesc();
}

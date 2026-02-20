package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {
    List<PostImage> findByPostIdInOrderByPostIdAscSortOrderAsc(Collection<UUID> postIds);
    List<PostImage> findByPostIdOrderBySortOrderAsc(UUID postId);
}

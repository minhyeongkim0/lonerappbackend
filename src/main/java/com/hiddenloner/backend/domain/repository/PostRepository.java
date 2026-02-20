package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByMissionIdOrderByCreatedAtDesc(UUID missionId);
    List<Post> findByStatusOrderByCreatedAtDesc(String status);

    Optional<Post> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    boolean existsByMissionIdAndUserId(UUID missionId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") UUID postId);

    @Query("""
        select p.missionId as missionId, count(p.id) as count
        from Post p
        where p.missionId in :missionIds
        group by p.missionId
    """)
    List<MissionCountRow> countByMissionIds(@Param("missionIds") Collection<UUID> missionIds);

    @Query("""
        select distinct p.missionId
        from Post p
        where p.userId = :userId
    """)
    List<UUID> findCompletedMissionIds(@Param("userId") UUID userId);

    interface MissionCountRow {
        UUID getMissionId();
        long getCount();
    }
}

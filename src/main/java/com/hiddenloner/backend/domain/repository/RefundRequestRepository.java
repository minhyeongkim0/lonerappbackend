package com.hiddenloner.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.RefundRequest;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    List<RefundRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<RefundRequest> findByStatusOrderByCreatedAtDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundRequest r where r.id = :refundId")
    Optional<RefundRequest> findByIdForUpdate(@Param("refundId") UUID refundId);
}

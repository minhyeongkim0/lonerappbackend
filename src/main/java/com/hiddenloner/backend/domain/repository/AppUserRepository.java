package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    boolean existsByUsername(String username);
    boolean existsByBankNameAndAccountNumber(String bankName, String accountNumber);
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findAllByIdIn(Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :userId")
    Optional<AppUser> findByIdForUpdate(@Param("userId") UUID userId);
}

package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    boolean existsByUsername(String username);
    boolean existsByBankNameAndAccountNumber(String bankName, String accountNumber);
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findAllByIdIn(Collection<UUID> ids);
}

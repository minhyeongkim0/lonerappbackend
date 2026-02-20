package com.hiddenloner.backend.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiddenloner.backend.domain.entity.BannedAccount;

public interface BannedAccountRepository extends JpaRepository<BannedAccount, UUID> {
    boolean existsByBankNameAndAccountNumber(String bankName, String accountNumber);
}

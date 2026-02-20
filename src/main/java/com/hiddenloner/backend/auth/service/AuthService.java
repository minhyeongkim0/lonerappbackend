package com.hiddenloner.backend.auth.service;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.auth.dto.AuthResponse;
import com.hiddenloner.backend.auth.dto.LoginRequest;
import com.hiddenloner.backend.auth.dto.MeResponse;
import com.hiddenloner.backend.auth.dto.RegisterRequest;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.config.SupabaseProperties;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.BannedAccountRepository;

@Service
public class AuthService {
    private static final Set<String> DEV_TEST_USERNAMES = Set.of("user1", "user2", "master");

    private final AppUserRepository appUserRepository;
    private final BannedAccountRepository bannedAccountRepository;
    private final SupabaseAuthClient supabaseAuthClient;
    private final SupabaseProperties supabaseProperties;

    public AuthService(
        AppUserRepository appUserRepository,
        BannedAccountRepository bannedAccountRepository,
        SupabaseAuthClient supabaseAuthClient,
        SupabaseProperties supabaseProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.bannedAccountRepository = bannedAccountRepository;
        this.supabaseAuthClient = supabaseAuthClient;
        this.supabaseProperties = supabaseProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.username().trim();
        String normalizedBankName = request.bankName().trim();
        String normalizedAccountNumber = normalizeAccount(request.accountNumber());

        validateRegisterUniqueness(normalizedUsername, normalizedBankName, normalizedAccountNumber);

        String email = toSupabaseEmail(normalizedUsername);
        UUID userId = supabaseAuthClient.signUp(email, request.password());

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername(normalizedUsername);
        user.setBirthYear((short) request.birthYear());
        user.setBirthMonth((short) request.birthMonth());
        user.setBirthDay((short) request.birthDay());
        user.setGender(request.gender());
        user.setBankName(normalizedBankName);
        user.setAccountNumber(normalizedAccountNumber);
        user.setPoint(0);
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());

        appUserRepository.save(user);

        SupabaseAuthClient.TokenResult tokenResult = supabaseAuthClient.login(email, request.password());
        return new AuthResponse(user.getId(), user.getUsername(), tokenResult.accessToken(), tokenResult.refreshToken(), tokenResult.expiresIn());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedUsername = request.username().trim();
        String effectivePassword = resolveLoginPassword(normalizedUsername, request.password());

        AppUser user = appUserRepository.findByUsername(normalizedUsername)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));

        SupabaseAuthClient.TokenResult tokenResult = supabaseAuthClient.login(toSupabaseEmail(normalizedUsername), effectivePassword);

        return new AuthResponse(user.getId(), user.getUsername(), tokenResult.accessToken(), tokenResult.refreshToken(), tokenResult.expiresIn());
    }

    public MeResponse me(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Authorization 헤더가 필요합니다.");
        }

        String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
        if (accessToken.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }

        UUID userId = supabaseAuthClient.getUserIdFromAccessToken(accessToken);

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        return new MeResponse(user.getId(), user.getUsername(), user.getPoint(), user.getBankName(), user.getAccountNumber());
    }

    private void validateRegisterUniqueness(String username, String bankName, String accountNumber) {
        if (appUserRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_DUPLICATE", "이미 사용 중인 아이디입니다.");
        }

        if (appUserRepository.existsByBankNameAndAccountNumber(bankName, accountNumber)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_DUPLICATE", "이미 가입된 계좌입니다.");
        }

        if (bannedAccountRepository.existsByBankNameAndAccountNumber(bankName, accountNumber)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_BANNED", "가입이 제한된 계좌입니다.");
        }
    }

    private String normalizeAccount(String accountNumber) {
        return accountNumber.replaceAll("[^0-9]", "");
    }

    private String toSupabaseEmail(String username) {
        return username + "@" + supabaseProperties.usernameEmailDomain();
    }

    // Supabase 최소 비밀번호 길이(6자) 제약 때문에 테스트 계정 로그인만 호환 처리.
    private String resolveLoginPassword(String username, String rawPassword) {
        if ("1234".equals(rawPassword) && DEV_TEST_USERNAMES.contains(username)) {
            return "123456";
        }
        return rawPassword;
    }
}

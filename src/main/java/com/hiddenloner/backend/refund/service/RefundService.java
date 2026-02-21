package com.hiddenloner.backend.refund.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.admin.dto.AdminRefundItemResponse;
import com.hiddenloner.backend.admin.dto.ProcessRefundResponse;
import com.hiddenloner.backend.admin.dto.RejectRefundRequest;
import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.config.AppAdminProperties;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.entity.PointHistory;
import com.hiddenloner.backend.domain.entity.RefundRequest;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.PointHistoryRepository;
import com.hiddenloner.backend.domain.repository.RefundRequestRepository;
import com.hiddenloner.backend.refund.dto.CreateRefundRequest;
import com.hiddenloner.backend.refund.dto.CreateRefundResponse;
import com.hiddenloner.backend.refund.dto.RefundRequestResponse;

@Service
public class RefundService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";

    private final RefundRequestRepository refundRequestRepository;
    private final AppUserRepository appUserRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SupabaseAuthClient supabaseAuthClient;
    private final AppAdminProperties appAdminProperties;

    public RefundService(
        RefundRequestRepository refundRequestRepository,
        AppUserRepository appUserRepository,
        PointHistoryRepository pointHistoryRepository,
        SupabaseAuthClient supabaseAuthClient,
        AppAdminProperties appAdminProperties
    ) {
        this.refundRequestRepository = refundRequestRepository;
        this.appUserRepository = appUserRepository;
        this.pointHistoryRepository = pointHistoryRepository;
        this.supabaseAuthClient = supabaseAuthClient;
        this.appAdminProperties = appAdminProperties;
    }

    @Transactional
    public CreateRefundResponse createRefund(CreateRefundRequest request, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        AppUser user = appUserRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        int amount = request.amount();
        if (amount <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REFUND_AMOUNT", "환급 금액은 1원 이상이어야 합니다.");
        }
        if (user.getPoint() < amount) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_POINT", "보유 포인트가 부족합니다.");
        }

        user.setPoint(user.getPoint() - amount);
        appUserRepository.save(user);

        OffsetDateTime now = OffsetDateTime.now();

        RefundRequest refund = new RefundRequest();
        refund.setId(UUID.randomUUID());
        refund.setUserId(user.getId());
        refund.setAmount(amount);
        refund.setStatus(STATUS_PENDING);
        refund.setBankName(user.getBankName());
        refund.setAccountNumber(user.getAccountNumber());
        refund.setCreatedAt(now);
        refundRequestRepository.save(refund);

        PointHistory pointHistory = new PointHistory();
        pointHistory.setId(UUID.randomUUID());
        pointHistory.setUserId(user.getId());
        pointHistory.setAmount(-amount);
        pointHistory.setReason("환급 신청 차감");
        pointHistory.setCreatedAt(now);
        pointHistoryRepository.save(pointHistory);

        return new CreateRefundResponse(toResponse(refund), user.getPoint());
    }

    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getMyRefunds(String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        return refundRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminRefundItemResponse> getPendingRefunds(String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        return refundRequestRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING).stream()
            .map(refund -> {
                AppUser user = appUserRepository.findById(refund.getUserId()).orElse(null);
                String username = user == null ? "unknown" : user.getUsername();
                return new AdminRefundItemResponse(
                    refund.getId(),
                    refund.getUserId(),
                    maskUsername(username),
                    refund.getAmount(),
                    refund.getBankName(),
                    refund.getAccountNumber(),
                    refund.getStatus(),
                    refund.getCreatedAt()
                );
            })
            .toList();
    }

    @Transactional
    public ProcessRefundResponse approveRefund(UUID refundId, String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "환급 신청을 찾을 수 없습니다."));

        if (!STATUS_PENDING.equals(refund.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REFUND_ALREADY_PROCESSED", "이미 처리된 환급 신청입니다.");
        }

        refund.setStatus(STATUS_APPROVED);
        refund.setProcessedAt(OffsetDateTime.now());
        refund.setProcessedBy(adminUserId);
        refundRequestRepository.save(refund);

        AppUser user = appUserRepository.findById(refund.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        return new ProcessRefundResponse(
            refund.getId(),
            refund.getStatus(),
            0,
            user.getPoint(),
            refund.getProcessedAt(),
            null
        );
    }

    @Transactional
    public ProcessRefundResponse rejectRefund(UUID refundId, RejectRefundRequest request, String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "환급 신청을 찾을 수 없습니다."));

        if (!STATUS_PENDING.equals(refund.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REFUND_ALREADY_PROCESSED", "이미 처리된 환급 신청입니다.");
        }

        AppUser user = appUserRepository.findByIdForUpdate(refund.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        user.setPoint(user.getPoint() + refund.getAmount());
        appUserRepository.save(user);

        OffsetDateTime now = OffsetDateTime.now();
        refund.setStatus(STATUS_REJECTED);
        refund.setProcessedAt(now);
        refund.setProcessedBy(adminUserId);
        String reason = normalizeReason(request == null ? null : request.reason());
        refund.setRejectedReason(reason);
        refundRequestRepository.save(refund);

        PointHistory pointHistory = new PointHistory();
        pointHistory.setId(UUID.randomUUID());
        pointHistory.setUserId(user.getId());
        pointHistory.setAmount(refund.getAmount());
        pointHistory.setReason("환급 반려 복구");
        pointHistory.setCreatedAt(now);
        pointHistoryRepository.save(pointHistory);

        return new ProcessRefundResponse(
            refund.getId(),
            refund.getStatus(),
            refund.getAmount(),
            user.getPoint(),
            refund.getProcessedAt(),
            refund.getRejectedReason()
        );
    }

    private String normalizeReason(String rawReason) {
        if (rawReason == null) {
            return null;
        }
        String normalized = rawReason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private RefundRequestResponse toResponse(RefundRequest refund) {
        return new RefundRequestResponse(
            refund.getId(),
            refund.getUserId(),
            refund.getAmount(),
            refund.getStatus(),
            refund.getBankName(),
            refund.getAccountNumber(),
            refund.getCreatedAt(),
            refund.getProcessedAt(),
            refund.getRejectedReason()
        );
    }

    private UUID resolveRequiredUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        return supabaseAuthClient.getUserIdFromAccessToken(token);
    }

    private void validateAdmin(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다."));

        List<String> admins = appAdminProperties.usernames() == null ? List.of() : appAdminProperties.usernames();
        boolean isAdmin = admins.stream()
            .map(name -> name == null ? "" : name.trim())
            .filter(name -> !name.isEmpty())
            .anyMatch(name -> name.equals(user.getUsername()));

        if (!isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다.");
        }
    }

    private String maskUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() <= 4) {
            return normalized + "_**";
        }
        return normalized.substring(0, 4) + "_**";
    }
}

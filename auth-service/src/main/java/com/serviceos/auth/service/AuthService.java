package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import com.serviceos.auth.dto.AccessTokenResponse;
import com.serviceos.auth.dto.CreateTechnicianRequest;
import com.serviceos.auth.dto.OtpSendRequest;
import com.serviceos.auth.dto.OtpVerifyRequest;
import com.serviceos.auth.entity.OtpSession;
import com.serviceos.auth.entity.RefreshToken;
import com.serviceos.auth.entity.User;
import com.serviceos.auth.repository.RefreshTokenRepository;
import com.serviceos.auth.repository.UserRepository;
import com.serviceos.shared.enums.Role;
import com.serviceos.shared.result.AuthResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RateLimitService rateLimit;
    private final TokenBlacklistService blacklist;
    private final AuthProperties props;

    public AuthService(UserRepository userRepo,
                       RefreshTokenRepository refreshRepo,
                       OtpService otpService,
                       JwtService jwtService,
                       RateLimitService rateLimit,
                       TokenBlacklistService blacklist,
                       AuthProperties props) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.rateLimit = rateLimit;
        this.blacklist = blacklist;
        this.props = props;
    }

    public AuthResult sendOtp(OtpSendRequest req) {
        RateLimitService.Result rl = rateLimit.tryRegisterOtpSend(req.phone());
        if (!rl.allowed()) {
            return new AuthResult.Failure(
                    "RATE_LIMITED",
                    "Too many OTP requests. Retry after %ds".formatted(rl.retryAfterSeconds())
            );
        }
        OtpSession session = otpService.createAndSend(req.phone(), req.purpose());
        return new AuthResult.OtpPending(req.phone(), session.getExpiresAt());
    }

    @Transactional
    public AuthResult verifyOtp(OtpVerifyRequest req) {
        OtpService.VerifyResult outcome = otpService.verify(req.phone(), req.purpose(), req.otp());
        AuthResult.Failure failure = switch (outcome) {
            case OK                 -> null;
            case MISMATCH           -> new AuthResult.Failure("INVALID_OTP", "OTP does not match");
            case TOO_MANY_ATTEMPTS  -> new AuthResult.Failure("TOO_MANY_ATTEMPTS", "Max OTP attempts exceeded");
            case NO_ACTIVE          -> new AuthResult.Failure("NO_ACTIVE_OTP", "No active OTP for this phone");
        };
        if (failure != null) {
            return failure;
        }

        User user = userRepo.findByPhone(req.phone())
                .orElseGet(() -> userRepo.save(User.create(req.phone(), Role.CUSTOMER, null)));

        if (!user.isActive()) {
            return new AuthResult.Failure("USER_BLOCKED", "User account is inactive");
        }

        return issueTokens(user, "otp-login");
    }

    @Transactional
    public AuthResult refresh(String refreshTokenRaw) {
        String hash = HashUtil.sha256(refreshTokenRaw);
        Optional<RefreshToken> rtOpt = refreshRepo.findByTokenHash(hash);
        if (rtOpt.isEmpty() || !rtOpt.get().isActive()) {
            return new AuthResult.Failure("INVALID_REFRESH", "Refresh token not valid");
        }
        RefreshToken old = rtOpt.get();

        Optional<User> userOpt = userRepo.findById(old.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            old.revoke();
            refreshRepo.save(old);
            return new AuthResult.Failure("USER_BLOCKED", "Owner of token is inactive");
        }
        old.revoke();
        refreshRepo.save(old);

        return issueTokens(userOpt.get(), old.getDeviceInfo());
    }

    @Transactional
    public void logout(UUID userId, String accessJti, Instant accessExpiry) {
        if (accessJti != null && accessExpiry != null) {
            blacklist.revoke(accessJti, accessExpiry);
        }
        refreshRepo.revokeAllForUser(userId);
    }

    @Transactional
    public AuthResult createTechnician(CreateTechnicianRequest req) {
        if (userRepo.existsByPhone(req.phone())) {
            return new AuthResult.Failure("PHONE_EXISTS", "User with this phone already exists");
        }
        User u = User.create(req.phone(), req.role(), req.name());
        u.setEmail(req.email());
        User saved = userRepo.save(u);
        return new AuthResult.Success(
                saved.getId(),
                saved.getPhone(),
                saved.getRole(),
                "",
                "",
                Instant.now()
        );
    }

    public AccessTokenResponse toAccessOnly(AuthResult.Success s) {
        return new AccessTokenResponse(s.accessToken(), s.refreshToken(), s.expiresAt());
    }

    private AuthResult issueTokens(User user, String deviceInfo) {
        JwtService.Issued access = jwtService.issueAccessToken(user.getId(), user.getPhone(), user.getRole());
        String refreshRaw = jwtService.issueRefreshTokenRaw();
        Instant refreshExp = jwtService.refreshTokenExpiry();
        RefreshToken rt = RefreshToken.create(user.getId(), HashUtil.sha256(refreshRaw), refreshExp, deviceInfo);
        refreshRepo.save(rt);
        return new AuthResult.Success(
                user.getId(),
                user.getPhone(),
                user.getRole(),
                access.token(),
                refreshRaw,
                access.expiresAt()
        );
    }
}

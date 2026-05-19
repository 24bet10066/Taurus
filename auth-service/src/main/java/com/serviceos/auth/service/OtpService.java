package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import com.serviceos.auth.entity.OtpSession;
import com.serviceos.auth.enums.OtpPurpose;
import com.serviceos.auth.repository.OtpSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpSessionRepository repo;
    private final TwoFactorClient sms;
    private final AuthProperties props;

    public OtpService(OtpSessionRepository repo, TwoFactorClient sms, AuthProperties props) {
        this.repo = repo;
        this.sms = sms;
        this.props = props;
    }

    @Transactional
    public OtpSession createAndSend(String phone, OtpPurpose purpose) {
        String otp = generateOtp(props.otp().length());
        Instant expiresAt = Instant.now().plus(props.otp().ttl());
        OtpSession session = OtpSession.create(phone, HashUtil.sha256(otp), purpose, expiresAt);
        OtpSession saved = repo.save(session);
        boolean sent = sms.sendOtp(phone, otp);
        if (!sent) {
            // Per spec: don't fail the request; log already done in TwoFactorClient.
            // The OTP record is persisted so dev can still verify if using console fallback.
        }
        return saved;
    }

    @Transactional
    public VerifyResult verify(String phone, OtpPurpose purpose, String submittedOtp) {
        Optional<OtpSession> active = repo.findActive(phone, purpose, Instant.now());
        if (active.isEmpty()) {
            return VerifyResult.NO_ACTIVE;
        }
        OtpSession session = active.get();
        if (session.getAttempts() >= props.otp().maxAttempts()) {
            session.markUsed();
            repo.save(session);
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }
        session.incrementAttempts();
        if (!HashUtil.sha256(submittedOtp).equals(session.getOtpHash())) {
            repo.save(session);
            return VerifyResult.MISMATCH;
        }
        session.markUsed();
        repo.save(session);
        return VerifyResult.OK;
    }

    private String generateOtp(int length) {
        int bound = (int) Math.pow(10, length);
        int n = RANDOM.nextInt(bound);
        return String.format("%0" + length + "d", n);
    }

    public enum VerifyResult { OK, MISMATCH, TOO_MANY_ATTEMPTS, NO_ACTIVE }
}

package com.serviceos.auth.service;

import com.serviceos.auth.config.AuthProperties;
import com.serviceos.auth.entity.OtpSession;
import com.serviceos.auth.enums.OtpPurpose;
import com.serviceos.auth.repository.OtpSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
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
        // Invalidate any prior active sessions for this (phone, purpose) so the
        // verify path never sees more than one candidate. Prevents the
        // NonUniqueResultException previously observed under rapid resend.
        int invalidated = repo.invalidateAllActive(phone, purpose);
        if (invalidated > 0) {
            log.debug("Invalidated {} stale OTP session(s) for phone ending {} purpose {}",
                    invalidated, mask(phone), purpose);
        }

        String otp = generateOtp(props.otp().length());
        Instant expiresAt = Instant.now().plus(props.otp().ttl());
        OtpSession session = OtpSession.create(phone, HashUtil.sha256(otp), purpose, expiresAt);
        OtpSession saved = repo.save(session);

        boolean sent = sms.sendOtp(phone, otp);
        if (!sent) {
            // Per spec: don't fail the request; log already done in TwoFactorClient.
            // The OTP record is persisted so dev can still verify if using console fallback.
            log.warn("OTP SMS dispatch returned false for phone ending {}", mask(phone));
        }
        return saved;
    }

    @Transactional
    public VerifyResult verify(String phone, OtpPurpose purpose, String submittedOtp) {
        List<OtpSession> active = repo.findAllActive(phone, purpose, Instant.now());
        if (active.isEmpty()) {
            return VerifyResult.NO_ACTIVE;
        }

        // Defensive: if more than one active session exists (e.g. legacy data
        // predating the createAndSend invalidation, or a race), keep the newest
        // and mark the rest as used so future verifies can't trip on them.
        OtpSession session = active.get(0);
        if (active.size() > 1) {
            log.warn("Found {} active OTP sessions for phone ending {} purpose {}; using newest, retiring rest",
                    active.size(), mask(phone), purpose);
            for (int i = 1; i < active.size(); i++) {
                OtpSession stale = active.get(i);
                stale.markUsed();
                repo.save(stale);
            }
        }

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

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    public enum VerifyResult { OK, MISMATCH, TOO_MANY_ATTEMPTS, NO_ACTIVE }
}

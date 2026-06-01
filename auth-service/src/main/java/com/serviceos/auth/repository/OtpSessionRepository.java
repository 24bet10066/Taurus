package com.serviceos.auth.repository;

import com.serviceos.auth.entity.OtpSession;
import com.serviceos.auth.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {

    /**
     * Returns all currently-active (unused, unexpired) OTP sessions for a (phone, purpose)
     * tuple, newest first. Returning a List instead of Optional makes the query resilient
     * to stale duplicates created by historical bugs or rapid resend clicks; the verify
     * path takes the newest and invalidates the rest.
     */
    @Query("""
            select s from OtpSession s
            where s.phone = :phone
              and s.purpose = :purpose
              and s.used = false
              and s.expiresAt > :now
            order by s.createdAt desc
            """)
    List<OtpSession> findAllActive(@Param("phone") String phone,
                                   @Param("purpose") OtpPurpose purpose,
                                   @Param("now") Instant now);

    /**
     * Convenience: returns the newest active session, or empty. Implemented as a default
     * method so the underlying List query stays the single source of truth.
     */
    default Optional<OtpSession> findActive(String phone, OtpPurpose purpose, Instant now) {
        List<OtpSession> all = findAllActive(phone, purpose, now);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /**
     * Marks every currently-active session for (phone, purpose) as used. Called before
     * issuing a new OTP so multiple active sessions never accumulate for the same target.
     * Returns the number of rows affected.
     */
    @Modifying
    @Query("""
            update OtpSession s
               set s.used = true
             where s.phone = :phone
               and s.purpose = :purpose
               and s.used = false
            """)
    int invalidateAllActive(@Param("phone") String phone,
                            @Param("purpose") OtpPurpose purpose);
}

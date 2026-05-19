package com.serviceos.auth.repository;

import com.serviceos.auth.entity.OtpSession;
import com.serviceos.auth.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {

    @Query("""
            select s from OtpSession s
            where s.phone = :phone
              and s.purpose = :purpose
              and s.used = false
              and s.expiresAt > :now
            order by s.createdAt desc
            """)
    Optional<OtpSession> findActive(@Param("phone") String phone,
                                    @Param("purpose") OtpPurpose purpose,
                                    @Param("now") Instant now);
}

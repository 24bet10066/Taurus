package com.serviceos.payment.repository;

import com.serviceos.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    Optional<PaymentRecord> findByJobId(UUID jobId);

    Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId);

    List<PaymentRecord> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    @Query("""
            SELECT p FROM PaymentRecord p
            WHERE p.status = 'COMPLETED'
              AND p.createdAt >= :from
              AND p.createdAt < :to
            """)
    List<PaymentRecord> findCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PaymentRecord p
            WHERE p.status = 'COMPLETED'
              AND p.paymentMethod = :method
              AND p.createdAt >= :from
              AND p.createdAt < :to
            """)
    BigDecimal sumByMethodAndRange(@Param("method") String method,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);
}

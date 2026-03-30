package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.retryCount < p.maxRetryCount")
    List<Payment> findRetryablePayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS'")
    long countSuccessfulPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status IN ('SUCCESS', 'FAILED')")
    long countTotalPayments();
}

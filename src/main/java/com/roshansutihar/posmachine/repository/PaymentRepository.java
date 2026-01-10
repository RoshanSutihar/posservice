package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.Payment;
import com.roshansutihar.posmachine.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);
    List<Payment> findByTransactionId(Long transactionId);
    List<Payment> findByPaymentDateBetween(OffsetDateTime start, OffsetDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentMethod = :paymentMethod AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal getTotalAmountByPaymentMethodAndDateRange(
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );


}

package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.QrPayment;
import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrPaymentRepository extends JpaRepository<QrPayment, Long> {
    Optional<QrPayment> findBySessionId(String sessionId);
    Optional<QrPayment> findByTransactionReference(String transactionReference);
    List<QrPayment> findByQrStatus(QrPaymentStatus status);
    List<QrPayment> findByMerchantId(String merchantId);

    @Query("SELECT qp FROM QrPayment qp WHERE qp.payment.transaction.id = :transactionId")
    Optional<QrPayment> findByTransactionId(@Param("transactionId") Long transactionId);

    boolean existsBySessionId(String sessionId);
    QrPayment findByPaymentId(Long paymentId);
}

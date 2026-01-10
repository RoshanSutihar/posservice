package com.roshansutihar.posmachine.entity;

import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "qr_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    @ToString.Exclude
    private Payment payment;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_status", length = 50)
    private QrPaymentStatus qrStatus;
}


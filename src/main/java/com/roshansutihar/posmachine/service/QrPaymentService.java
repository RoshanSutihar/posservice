package com.roshansutihar.posmachine.service;

import com.roshansutihar.posmachine.entity.Payment;
import com.roshansutihar.posmachine.entity.QrPayment;
import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import com.roshansutihar.posmachine.repository.PaymentRepository;
import com.roshansutihar.posmachine.repository.QrPaymentRepository;
import com.roshansutihar.posmachine.request.QrPaymentRequest;
import com.roshansutihar.posmachine.response.QrPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QrPaymentService {
    private final QrPaymentRepository qrPaymentRepository;
    private final PaymentRepository paymentRepository;

    public QrPaymentResponse initiateQrPayment(QrPaymentRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        QrPayment qrPayment = QrPayment.builder()
                .payment(payment)
                .merchantId(request.getMerchantId())
                .sessionId(generateSessionId())
                .paidAmount(request.getPaidAmount())
                .qrStatus(QrPaymentStatus.valueOf(String.valueOf(QrPaymentStatus.PENDING)))
                .build();

        QrPayment saved = qrPaymentRepository.save(qrPayment);
        return mapToQrPaymentResponse(saved);
    }

    public QrPaymentResponse updateQrPaymentStatus(Long qrPaymentId, QrPaymentStatus status) {
        QrPayment qrPayment = qrPaymentRepository.findById(qrPaymentId)
                .orElseThrow(() -> new RuntimeException("QR Payment not found"));

        qrPayment.setQrStatus(QrPaymentStatus.valueOf(String.valueOf(status)));
        QrPayment updated = qrPaymentRepository.save(qrPayment);
        return mapToQrPaymentResponse(updated);
    }

    public QrPaymentResponse getQrPaymentBySessionId(String sessionId) {
        QrPayment qrPayment = qrPaymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("QR Payment not found"));
        return mapToQrPaymentResponse(qrPayment);
    }

    public List<QrPaymentResponse> getPendingQrPayments() {
        return qrPaymentRepository.findByQrStatus(QrPaymentStatus.PENDING).stream()
                .map(this::mapToQrPaymentResponse)
                .collect(Collectors.toList());
    }

    private String generateSessionId() {
        return "SESSION_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private QrPaymentResponse mapToQrPaymentResponse(QrPayment qrPayment) {
        return QrPaymentResponse.builder()
                .id(qrPayment.getId())
                .paymentId(qrPayment.getPayment().getId())
                .merchantId(qrPayment.getMerchantId())
                .sessionId(qrPayment.getSessionId())
                .paidAmount(qrPayment.getPaidAmount())
                .transactionReference(qrPayment.getTransactionReference())
                .status(QrPaymentStatus.valueOf(String.valueOf(qrPayment.getQrStatus())))
                .build();
    }
}
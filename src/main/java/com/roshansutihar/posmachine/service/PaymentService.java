package com.roshansutihar.posmachine.service;

import com.roshansutihar.posmachine.entity.Payment;
import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.enums.PaymentMethod;
import com.roshansutihar.posmachine.repository.PaymentRepository;
import com.roshansutihar.posmachine.repository.TransactionsRepository;
import com.roshansutihar.posmachine.request.PaymentRequest;
import com.roshansutihar.posmachine.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final TransactionsRepository transactionRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        Transactions transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Payment payment = Payment.builder()
                .transaction(transaction)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .build();

        Payment saved = paymentRepository.save(payment);
        return mapToPaymentResponse(saved);
    }

    public List<PaymentResponse> getPaymentsByTransaction(Long transactionId) {
        return paymentRepository.findByTransactionId(transactionId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByMethod(PaymentMethod method) {
        return paymentRepository.findByPaymentMethod(method).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransaction().getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}

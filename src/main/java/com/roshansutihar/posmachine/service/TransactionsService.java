package com.roshansutihar.posmachine.service;

import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.repository.ProductRepository;
import com.roshansutihar.posmachine.repository.TransactionsRepository;
import com.roshansutihar.posmachine.request.TransactionsRequest;
import com.roshansutihar.posmachine.response.TransactionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsRepository transactionRepository;
    private final ProductRepository productRepository;

    public TransactionsResponse createTransaction(TransactionsRequest request) {
        Transactions transaction = Transactions.builder()
                .transactionType(request.getTransactionType())
                .totalAmount(request.getTotalAmount())
                .build();

        Transactions saved = transactionRepository.save(transaction);
        return mapToTransactionResponse(saved);
    }

    public TransactionsResponse getTransactionById(Long id) {
        Transactions transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return mapToTransactionResponse(transaction);
    }

    public List<TransactionsResponse> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        OffsetDateTime startDate = start.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDate = end.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        return transactionRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private TransactionsResponse mapToTransactionResponse(Transactions transaction) {
        return TransactionsResponse.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .totalAmount(transaction.getTotalAmount())
                .build();
    }
}
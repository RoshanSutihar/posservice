package com.roshansutihar.posmachine.service;

import com.roshansutihar.posmachine.entity.Product;
import com.roshansutihar.posmachine.entity.TransactionItem;
import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.repository.ProductRepository;
import com.roshansutihar.posmachine.repository.TransactionItemRepository;
import com.roshansutihar.posmachine.repository.TransactionsRepository;
import com.roshansutihar.posmachine.request.TransactionsItemRequest;
import com.roshansutihar.posmachine.response.TransactionsItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionItemService {
    private final TransactionItemRepository transactionItemRepository;
    private final TransactionsRepository transactionRepository;
    private final ProductRepository productRepository;

    public TransactionsItemResponse addItemToTransaction(TransactionsItemRequest request) {
        Transactions transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        TransactionItem item = TransactionItem.builder()
                .transaction(transaction)
                .product(product)
                .quantity(request.getQuantity())
                .priceAtSale(request.getPriceAtSale())
                .build();

        TransactionItem saved = transactionItemRepository.save(item);
        return mapToTransactionItemResponse(saved);
    }

    public List<TransactionsItemResponse> getItemsByTransaction(Long transactionId) {
        return transactionItemRepository.findByTransactionId(transactionId).stream()
                .map(this::mapToTransactionItemResponse)
                .collect(Collectors.toList());
    }

    private TransactionsItemResponse mapToTransactionItemResponse(TransactionItem item) {
        return TransactionsItemResponse.builder()
                .id(item.getId())
                .transactionId(item.getTransaction().getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .priceAtSale(item.getPriceAtSale())
                .subtotal(item.getSubtotal())
                .build();
    }
}
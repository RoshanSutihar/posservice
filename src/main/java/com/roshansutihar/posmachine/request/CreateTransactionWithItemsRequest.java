package com.roshansutihar.posmachine.request;

import com.roshansutihar.posmachine.enums.TransactionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionWithItemsRequest {
    @NotNull
    private TransactionType transactionType;

    @NotEmpty
    private List<TransactionsItemRequest> items;

    @NotNull
    private PaymentRequest payment;
}
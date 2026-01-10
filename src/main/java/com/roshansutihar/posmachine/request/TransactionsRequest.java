package com.roshansutihar.posmachine.request;

import com.roshansutihar.posmachine.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsRequest {
    @NotNull
    private TransactionType transactionType;

    @NotNull
    @Positive
    private BigDecimal totalAmount;
}

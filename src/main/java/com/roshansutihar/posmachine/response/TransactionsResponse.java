package com.roshansutihar.posmachine.response;

import com.roshansutihar.posmachine.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsResponse {
    private Long id;
    private OffsetDateTime transactionDate;
    private TransactionType transactionType;
    private BigDecimal totalAmount;
}

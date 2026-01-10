package com.roshansutihar.posmachine.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWithDetailsResponse {
    private TransactionsResponse transaction;
    private List<TransactionsItemResponse> items;
    private List<PaymentResponse> payments;
}

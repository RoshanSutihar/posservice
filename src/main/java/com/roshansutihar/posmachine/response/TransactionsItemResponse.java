package com.roshansutihar.posmachine.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsItemResponse {
    private Long id;
    private Long transactionId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtSale;
    private BigDecimal subtotal;
}

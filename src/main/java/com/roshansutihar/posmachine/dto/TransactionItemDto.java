package com.roshansutihar.posmachine.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionItemDto {
    private String productName;
    private Integer quantity;
    private BigDecimal subtotal;
}

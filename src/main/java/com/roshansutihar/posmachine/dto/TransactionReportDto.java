package com.roshansutihar.posmachine.dto;

import com.roshansutihar.posmachine.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionReportDto {
    private Long id;
    private OffsetDateTime transactionDate;
    private TransactionType transactionType;
    private BigDecimal totalAmount;
    private List<TransactionItemDto> transactionItems;
}

package com.roshansutihar.posmachine.response;

import com.roshansutihar.posmachine.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PaymentResponse {
    private Long id;
    private Long transactionId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private OffsetDateTime paymentDate;
}

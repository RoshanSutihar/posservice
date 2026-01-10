package com.roshansutihar.posmachine.request;

import com.roshansutihar.posmachine.enums.PaymentMethod;
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
public class PaymentRequest {
    @NotNull
    private Long transactionId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Positive
    private BigDecimal amount;
}

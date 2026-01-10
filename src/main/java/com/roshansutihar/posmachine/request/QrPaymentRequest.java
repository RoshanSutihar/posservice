package com.roshansutihar.posmachine.request;

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
public class QrPaymentRequest {
    @NotNull
    private Long paymentId;

    private String merchantId;

    @NotNull
    @Positive
    private BigDecimal paidAmount;
}

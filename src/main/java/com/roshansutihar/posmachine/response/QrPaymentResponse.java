package com.roshansutihar.posmachine.response;

import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentResponse {
    private Long id;
    private Long paymentId;
    private String merchantId;
    private String sessionId;
    private BigDecimal paidAmount;
    private String transactionReference;
    private QrPaymentStatus status;
}

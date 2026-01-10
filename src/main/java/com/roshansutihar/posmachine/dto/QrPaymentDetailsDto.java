package com.roshansutihar.posmachine.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentDetailsDto {
    private String terminalId;
    private String merchantId;
    private String sessionId;
    private String transactionReference;
    private String qrStatus;
}

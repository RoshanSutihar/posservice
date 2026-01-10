package com.roshansutihar.posmachine.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentConfigDTO {
    private String merchantId;
    private String terminalId;
    private String apiUrl;

}

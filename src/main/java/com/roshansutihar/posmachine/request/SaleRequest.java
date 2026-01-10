package com.roshansutihar.posmachine.request;

import com.roshansutihar.posmachine.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequest {
    private List<CartItem> cartItems;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
}

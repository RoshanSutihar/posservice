package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.entity.*;
import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import com.roshansutihar.posmachine.enums.TransactionType;
import com.roshansutihar.posmachine.repository.*;
import com.roshansutihar.posmachine.request.CartItem;
import com.roshansutihar.posmachine.request.SaleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class PosController {
    private final ProductRepository productRepository;
    private final TransactionsRepository transactionsRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final QrPaymentRepository qrPaymentRepository;

    @GetMapping
    public String posPage(Model model) {
        model.addAttribute("cartItems", new ArrayList<CartItem>());
        model.addAttribute("total", BigDecimal.ZERO);
        return "pos";
    }


    @PostMapping("/complete-sale")
    @ResponseBody
    public ResponseEntity<?> completeSale(@RequestBody SaleRequest saleRequest) {
        try {
            // 1. Create transaction items
            List<TransactionItem> transactionItems = new ArrayList<>();

            for (CartItem cartItem : saleRequest.getCartItems()) {
                Product product = productRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));

                TransactionItem item = TransactionItem.builder()
                        .product(product)
                        .quantity(cartItem.getQuantity())
                        .priceAtSale(product.getPrice())
                        .subtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                        .build();

                transactionItems.add(item);
            }

            // 2. Create transaction
            Transactions transaction = Transactions.builder()
                    .transactionType(TransactionType.SALE)
                    .totalAmount(saleRequest.getTotalAmount())
                    .transactionDate(OffsetDateTime.now())
                    .transactionItems(transactionItems)
                    .build();

            // Link items to transaction
            Transactions finalTransaction = transaction;
            transactionItems.forEach(item -> item.setTransaction(finalTransaction));

            // Save transaction
            transaction = transactionsRepository.save(transaction);

            // 3. Create payment
            Payment payment = Payment.builder()
                    .transaction(transaction)
                    .paymentMethod(saleRequest.getPaymentMethod())
                    .amount(saleRequest.getTotalAmount())
                    .paymentDate(OffsetDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            // 4. If QR payment, create QR payment record
            if ("QR".equals(saleRequest.getPaymentMethod()) && saleRequest.getQrPaymentDetails() != null) {
                QrPayment qrPayment = QrPayment.builder()
                        .payment(payment) // Link to payment
                        .terminalId(saleRequest.getQrPaymentDetails().getTerminalId())
                        .merchantId(saleRequest.getQrPaymentDetails().getMerchantId())
                        .sessionId(saleRequest.getQrPaymentDetails().getSessionId())
                        .paidAmount(saleRequest.getTotalAmount())
                        .transactionReference(saleRequest.getQrPaymentDetails().getTransactionReference())
                        .qrStatus(QrPaymentStatus.COMPLETED)
                        .build();

                // Save QR payment (you'll need QrPaymentRepository)
                qrPaymentRepository.save(qrPayment);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionId", transaction.getId(),
                    "message", "Sale completed successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error completing sale: " + e.getMessage()
                    ));
        }
    }
}
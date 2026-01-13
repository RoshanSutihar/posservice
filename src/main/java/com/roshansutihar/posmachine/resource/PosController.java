package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.entity.*;
import com.roshansutihar.posmachine.enums.PaymentMethod;
import com.roshansutihar.posmachine.enums.QrPaymentStatus;
import com.roshansutihar.posmachine.enums.TransactionType;
import com.roshansutihar.posmachine.repository.*;
import com.roshansutihar.posmachine.request.CartItem;
import com.roshansutihar.posmachine.request.SaleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public ResponseEntity<?> completeSale(@RequestBody SaleRequest saleRequest) {
        // --- LOGGING START ---
        System.out.println("====== SALE ATTEMPT LOG ======");
        System.out.println("Payment Method Received: " + saleRequest.getPaymentMethod());
        System.out.println("Is QR Method? " + (PaymentMethod.QR.equals(saleRequest.getPaymentMethod())));
        System.out.println("QR Details Object null? " + (saleRequest.getQrPaymentDetails() == null));

        if (saleRequest.getQrPaymentDetails() != null) {
            System.out.println("Ref inside Details: " + saleRequest.getQrPaymentDetails().getTransactionReference());
        }
        System.out.println("==============================");
        // --- LOGGING END ---

        try {
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

            Transactions transaction = Transactions.builder()
                    .transactionType(TransactionType.SALE)
                    .totalAmount(saleRequest.getTotalAmount())
                    .transactionDate(OffsetDateTime.now())
                    .build();

            Transactions finalTransaction = transaction;
            transactionItems.forEach(item -> item.setTransaction(finalTransaction));
            transaction.setTransactionItems(transactionItems);

            transaction = transactionsRepository.save(transaction);

            Payment payment = Payment.builder()
                    .transaction(transaction)
                    .paymentMethod(saleRequest.getPaymentMethod())
                    .amount(saleRequest.getTotalAmount())
                    .paymentDate(OffsetDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            // This block is what we are debugging
            if (PaymentMethod.QR.equals(saleRequest.getPaymentMethod()) && saleRequest.getQrPaymentDetails() != null) {
                System.out.println("SUCCESS: Entering QR Insert Block...");
                QrPayment qrPayment = QrPayment.builder()
                        .payment(payment)
                        .terminalId(saleRequest.getQrPaymentDetails().getTerminalId())
                        .merchantId(saleRequest.getQrPaymentDetails().getMerchantId())
                        .sessionId(saleRequest.getQrPaymentDetails().getSessionId())
                        .paidAmount(saleRequest.getTotalAmount())
                        .transactionReference(saleRequest.getQrPaymentDetails().getTransactionReference())
                        .qrStatus(QrPaymentStatus.COMPLETED)
                        .build();

                qrPaymentRepository.save(qrPayment);
                System.out.println("SUCCESS: QrPayment Saved.");
            } else {
                System.out.println("SKIPPED: QR Insert block was not entered.");
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionId", transaction.getId(),
                    "message", "Sale completed successfully"
            ));

        } catch (Exception e) {
            System.err.println("ERROR in completeSale: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.dto.QrPaymentDetailsDto;
import com.roshansutihar.posmachine.dto.TransactionItemDto;
import com.roshansutihar.posmachine.dto.TransactionReportDto;
import com.roshansutihar.posmachine.entity.Payment;
import com.roshansutihar.posmachine.entity.QrPayment;
import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.enums.PaymentMethod;
import com.roshansutihar.posmachine.repository.PaymentRepository;
import com.roshansutihar.posmachine.repository.QrPaymentRepository;
import com.roshansutihar.posmachine.repository.TransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final TransactionsRepository transactionsRepository;
    private final QrPaymentRepository qrPaymentRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public String salesReport(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {
        List<Transactions> transactions;

        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            transactions = transactionsRepository.findByTransactionDateBetween(
                    start.atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
            );
        } else {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            transactions = transactionsRepository.findByTransactionDateBetween(
                    start.atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
            );
        }

        // Fetch payments for each transaction to check payment method
        Map<Long, Payment> transactionPayments = new HashMap<>();
        Map<Long, QrPayment> transactionQrPayments = new HashMap<>();

        for (Transactions transaction : transactions) {
            // Get payment for this transaction - returns List, take first one
            List<Payment> payments = paymentRepository.findByTransactionId(transaction.getId());
            if (payments != null && !payments.isEmpty()) {
                Payment payment = payments.get(0); // Take first payment
                transactionPayments.put(transaction.getId(), payment);

                // If it's a QR payment, get QR details
                if (payment.getPaymentMethod() == PaymentMethod.QR) { // Use enum comparison
                    QrPayment qrPayment = qrPaymentRepository.findByPaymentId(payment.getId());
                    if (qrPayment != null) {
                        transactionQrPayments.put(transaction.getId(), qrPayment);
                    }
                }
            }
        }

        List<TransactionReportDto> transactionDtos = transactions.stream()
                .map(transaction -> convertToDto(transaction,
                        transactionPayments.get(transaction.getId()),
                        transactionQrPayments.get(transaction.getId())))
                .collect(Collectors.toList());

        model.addAttribute("transactions", transactionDtos);

        BigDecimal totalSales = transactions.stream()
                .map(Transactions::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalSales", totalSales);

        return "reports";
    }

    private TransactionReportDto convertToDto(Transactions transaction,
                                              Payment payment,
                                              QrPayment qrPayment) {
        List<TransactionItemDto> itemDtos = transaction.getTransactionItems().stream()
                .map(item -> TransactionItemDto.builder()
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return TransactionReportDto.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .totalAmount(transaction.getTotalAmount())
                .transactionItems(itemDtos)
                .paymentMethod(payment != null ? payment.getPaymentMethod().name() : null) // Convert enum to string
                .qrPaymentDetails(qrPayment != null ?
                        QrPaymentDetailsDto.builder()
                                .terminalId(qrPayment.getTerminalId())
                                .merchantId(qrPayment.getMerchantId())
                                .sessionId(qrPayment.getSessionId())
                                .transactionReference(qrPayment.getTransactionReference())
                                .qrStatus(qrPayment.getQrStatus() != null ? qrPayment.getQrStatus().name() : null) // Convert enum to string
                                .build()
                        : null)
                .build();
    }
}
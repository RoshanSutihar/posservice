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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    @GetMapping
    @Transactional(readOnly = true) // Ensures LAZY loaded fields are accessible during mapping
    public String salesReport(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {

        List<Transactions> transactions;
        OffsetDateTime startDateTime;
        OffsetDateTime endDateTime;

        // 1. Handle Date Filtering Logic
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            startDateTime = LocalDate.parse(startDate).atStartOfDay().atOffset(ZoneOffset.UTC);
            endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        } else {
            // Default to last 30 days if no filter provided
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            startDateTime = start.atStartOfDay().atOffset(ZoneOffset.UTC);
            endDateTime = end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        }

        // 2. Fetch Transactions using the JOIN FETCH method in your Repository
        transactions = transactionsRepository.findByTransactionDateBetween(startDateTime, endDateTime);

        // 3. Map Entities to DTOs
        List<TransactionReportDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 4. Calculate Total Summary
        BigDecimal totalSales = transactions.stream()
                .map(Transactions::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Add to Model for Thymeleaf
        model.addAttribute("transactions", transactionDtos);
        model.addAttribute("totalSales", totalSales);

        return "reports";
    }

    /**
     * Helper method to convert Entity graph to DTO.
     * Since we used JOIN FETCH, the payments and qrPayment relationships
     * are already populated in the objects.
     */
    private TransactionReportDto convertToDto(Transactions transaction) {
        // Get the first payment associated with this transaction
        Payment payment = transaction.getPayments().stream()
                .findFirst()
                .orElse(null);

        // Get QR details directly from the payment relationship
        QrPayment qrPayment = (payment != null) ? payment.getQrPayment() : null;

        // Map the items to DTOs
        List<TransactionItemDto> itemDtos = transaction.getTransactionItems().stream()
                .map(item -> TransactionItemDto.builder()
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        // Build the final report DTO
        return TransactionReportDto.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .totalAmount(transaction.getTotalAmount())
                .transactionItems(itemDtos)
                .paymentMethod(payment != null ? payment.getPaymentMethod().name() : "N/A")
                .qrPaymentDetails(qrPayment != null ?
                        QrPaymentDetailsDto.builder()
                                .terminalId(qrPayment.getTerminalId())
                                .merchantId(qrPayment.getMerchantId())
                                .sessionId(qrPayment.getSessionId())
                                .transactionReference(qrPayment.getTransactionReference())
                                .qrStatus(qrPayment.getQrStatus() != null ? qrPayment.getQrStatus().name() : null)
                                .build()
                        : null)
                .build();
    }
}
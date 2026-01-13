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
    @Transactional(readOnly = true)
    public String salesReport(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {

        List<Transactions> transactions;
        OffsetDateTime startDateTime;
        OffsetDateTime endDateTime;

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            startDateTime = LocalDate.parse(startDate).atStartOfDay().atOffset(ZoneOffset.UTC);
            endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        } else {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            startDateTime = start.atStartOfDay().atOffset(ZoneOffset.UTC);
            endDateTime = end.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        }

        // This call no longer crashes because we only fetch ONE 'bag' (payments)
        transactions = transactionsRepository.findByTransactionDateBetween(startDateTime, endDateTime);

        List<TransactionReportDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        BigDecimal totalSales = transactions.stream()
                .map(Transactions::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("transactions", transactionDtos);
        model.addAttribute("totalSales", totalSales);

        return "reports";
    }

    private TransactionReportDto convertToDto(Transactions transaction) {
        // Since we JOIN FETCHed payments and qrPayment, these are ready
        Payment payment = transaction.getPayments().stream()
                .findFirst()
                .orElse(null);

        QrPayment qrPayment = (payment != null) ? payment.getQrPayment() : null;

        // transactionItems will be fetched here via the Transactional session
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
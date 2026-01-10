package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.dto.TransactionItemDto;
import com.roshansutihar.posmachine.dto.TransactionReportDto;
import com.roshansutihar.posmachine.entity.Transactions;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final TransactionsRepository transactionsRepository;

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


        List<TransactionReportDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        model.addAttribute("transactions", transactionDtos);

        BigDecimal totalSales = transactions.stream()
                .map(Transactions::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalSales", totalSales);

        return "reports";
    }

    private TransactionReportDto convertToDto(Transactions transaction) {
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
                .build();
    }
}

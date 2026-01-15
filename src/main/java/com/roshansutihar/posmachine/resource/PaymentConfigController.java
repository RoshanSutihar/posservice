package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.dto.PaymentConfigDTO;
import com.roshansutihar.posmachine.entity.StoreInfo;
import com.roshansutihar.posmachine.repository.StoreInfoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/config")
public class PaymentConfigController {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfigController.class);
    private final StoreInfoRepository storeInfoRepository;

    public PaymentConfigController(StoreInfoRepository storeInfoRepository) {
        this.storeInfoRepository = storeInfoRepository;
    }

    @GetMapping("/payment")
    public ResponseEntity<PaymentConfigDTO> getPaymentConfig() {
        log.info("=== GET PAYMENT CONFIG (POS) ===");

        StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> {
                    log.error("Store configuration not found in database");
                    return new RuntimeException("Store configuration not found. Please set it up first.");
                });

        log.info("Database values retrieved:");
        log.info("- Merchant ID: {}", store.getMerchantId());
        log.info("- Terminal ID: {}", store.getTerminalId());
        log.info("- API URL from DB (for display): {}", store.getApiUrl());

        PaymentConfigDTO dto = new PaymentConfigDTO();
        dto.setMerchantId(store.getMerchantId());
        dto.setTerminalId(store.getTerminalId());

        // Use stored API URL for display only
        dto.setApiUrl(store.getApiUrl());

        log.info("Returning DTO with stored API URL for display");
        return ResponseEntity.ok(dto);
    }
}
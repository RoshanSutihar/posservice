package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.dto.PaymentConfigDTO;
import com.roshansutihar.posmachine.entity.StoreInfo;
import com.roshansutihar.posmachine.repository.StoreInfoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class PaymentConfigController {

    private final StoreInfoRepository storeInfoRepository;

    public PaymentConfigController(StoreInfoRepository storeInfoRepository) {
        this.storeInfoRepository = storeInfoRepository;
    }

    @GetMapping("/payment")
    public ResponseEntity<PaymentConfigDTO> getPaymentConfig() {
        // Get the store config (we assume there's only one store)
        StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Store configuration not found. Please set it up first."));

        PaymentConfigDTO dto = new PaymentConfigDTO();
        dto.setMerchantId(store.getMerchantId());
        dto.setTerminalId(store.getTerminalId());
        dto.setApiUrl(store.getApiUrl());  // example: "http://localhost:8091" or real gateway url

        return ResponseEntity.ok(dto);
    }
}

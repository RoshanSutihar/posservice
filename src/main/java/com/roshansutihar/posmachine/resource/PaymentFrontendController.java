package com.roshansutihar.posmachine.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roshansutihar.posmachine.entity.StoreInfo;
import com.roshansutihar.posmachine.repository.StoreInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentFrontendController {

    private static final Logger log = LoggerFactory.getLogger(PaymentFrontendController.class);

    // YOUR EXACT HARCODED URL - NO CHANGES
    private static final String HARCODED_GATEWAY_BASE_URL = "http://payments.roshansutihar.com.np:2011";
    private static final String HARCODED_TERMINAL_ID = "DEFAULT_TERMINAL";

    private final StoreInfoRepository storeInfoRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public PaymentFrontendController(StoreInfoRepository storeInfoRepository, RestTemplate restTemplate) {
        this.storeInfoRepository = storeInfoRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        log.info("=== GET PAYMENT CONFIG FROM FRONTEND CONTROLLER ===");

        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> {
                        log.error("Store configuration not found in DB");
                        return new RuntimeException("Store configuration missing");
                    });

            log.info("Database values:");
            log.info("- Merchant ID: {}", store.getMerchantId());
            log.info("- API URL in DB: {}", store.getApiUrl());
            log.info("- Terminal ID in DB: {}", store.getTerminalId());

            Map<String, Object> config = new HashMap<>();
            config.put("merchantId", store.getMerchantId());

            // Return stored API URL for display only
            config.put("apiUrl", store.getApiUrl());

            // Use hardcoded terminal ID if not in DB
            String terminalId = store.getTerminalId() != null ? store.getTerminalId() : HARCODED_TERMINAL_ID;
            config.put("terminalId", terminalId);

            log.info("Returning config for display - merchantId={}, terminalId={}, apiUrl={}",
                    store.getMerchantId(), terminalId, store.getApiUrl());

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("Error loading config: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load configuration: " + e.getMessage()));
        }
    }

    @PostMapping("/initiate-qr")
    public ResponseEntity<?> initiateQrPayment(@RequestBody Map<String, Object> requestBody) {
        log.info("=== INITIATE QR PAYMENT ===");
        log.info("Request body: {}", requestBody);

        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> {
                        log.error("Store configuration missing from database");
                        return new RuntimeException("Store configuration missing");
                    });

            log.info("Store configuration loaded:");
            log.info("- Merchant ID: {}", store.getMerchantId());
            log.info("- Terminal ID: {}", store.getTerminalId());
            log.info("- Secret Key exists: {}", store.getSecretKey() != null);

            // Validate required fields
            if (store.getMerchantId() == null || store.getMerchantId().trim().isEmpty()) {
                log.error("Merchant ID is null or empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Merchant ID not configured"));
            }

            if (store.getSecretKey() == null || store.getSecretKey().trim().isEmpty()) {
                log.error("Secret Key is null or empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Secret Key not configured"));
            }

            // Prepare data for signature
            Map<String, Object> ordered = new LinkedHashMap<>();
            ordered.put("terminalId", requestBody.get("terminalId"));
            ordered.put("amount", requestBody.get("amount"));
            ordered.put("transactionRef", requestBody.get("transactionRef"));
            ordered.put("callbackUrl", requestBody.get("callbackUrl"));

            String jsonString = new ObjectMapper().writeValueAsString(ordered);
            String dataToSign = store.getMerchantId() + jsonString;

            // Generate HMAC-SHA256 signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    Base64.getDecoder().decode(store.getSecretKey()), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(signatureBytes);

            log.info("Generated signature: {}", signature);

            // USE YOUR HARCODED URL FOR API CALLS
            String gatewayUrl = HARCODED_GATEWAY_BASE_URL + "/api/v1/payments/initiate";
            log.info("USING HARCODED GATEWAY URL: {}", gatewayUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Merchant-ID", store.getMerchantId());
            headers.set("X-Signature", signature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending payment request...");
            ResponseEntity<Map> gatewayResponse = restTemplate.postForEntity(gatewayUrl, entity, Map.class);

            log.info("Payment response status: {}", gatewayResponse.getStatusCode());

            return ResponseEntity.status(gatewayResponse.getStatusCode())
                    .body(gatewayResponse.getBody());

        } catch (Exception e) {
            log.error("Error initiating QR payment: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to initiate payment: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String sessionId) {
        log.info("=== CHECK PAYMENT STATUS ===");
        log.info("Session ID: {}", sessionId);

        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> {
                        log.error("Store configuration missing from database");
                        return new RuntimeException("Store configuration missing");
                    });

            // USE YOUR HARCODED URL FOR API CALLS
            String gatewayUrl = HARCODED_GATEWAY_BASE_URL + "/api/v1/payments/status/" + sessionId;
            log.info("USING HARCODED GATEWAY URL: {}", gatewayUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add authentication if needed
            if (store.getMerchantId() != null && store.getSecretKey() != null) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String dataToSign = store.getMerchantId() + sessionId + timestamp;

                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(
                        Base64.getDecoder().decode(store.getSecretKey()), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
                String signature = Base64.getEncoder().encodeToString(signatureBytes);

                headers.set("X-Merchant-ID", store.getMerchantId());
                headers.set("X-Timestamp", timestamp);
                headers.set("X-Signature", signature);
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> gatewayResponse = restTemplate.exchange(
                    gatewayUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.info("Payment status response: {}", gatewayResponse.getStatusCode());

            return ResponseEntity.ok(gatewayResponse.getBody());

        } catch (Exception e) {
            log.error("Error checking payment status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error: " + e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }
}
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentFrontendController {

    private static final Logger log = LoggerFactory.getLogger(PaymentFrontendController.class);

    // HARDCODED PAYMENT GATEWAY URL
    private static final String HARDCODED_GATEWAY_BASE_URL = "http://payments.roshansutihar.com.np:2011";
    private static final String HARDCODED_TERMINAL_ID = "DEFAULT_TERMINAL";

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

            // IMPORTANT: Return stored API URL for display
            config.put("apiUrl", store.getApiUrl());

            // Use hardcoded terminal ID if not in DB
            String terminalId = store.getTerminalId() != null ? store.getTerminalId() : HARDCODED_TERMINAL_ID;
            config.put("terminalId", terminalId);

            log.info("Returning config: merchantId={}, terminalId={}, apiUrl={}",
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

            // Prepare data for signature
            Map<String, Object> ordered = new LinkedHashMap<>();
            ordered.put("terminalId", requestBody.get("terminalId"));
            ordered.put("amount", requestBody.get("amount"));
            ordered.put("transactionRef", requestBody.get("transactionRef"));
            ordered.put("callbackUrl", requestBody.get("callbackUrl"));

            String jsonString = new ObjectMapper().writeValueAsString(ordered);
            String dataToSign = store.getMerchantId() + jsonString;

            log.info("Data to sign: {}", dataToSign);

            // Generate HMAC-SHA256 signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    Base64.getDecoder().decode(store.getSecretKey()), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(signatureBytes);

            log.info("Generated signature: {}", signature);

            // IMPORTANT: Use HARDCODED URL for gateway call
            String gatewayUrl = HARDCODED_GATEWAY_BASE_URL + "/api/v1/payments/initiate";
            log.info("Using HARDCODED gateway URL: {}", gatewayUrl);

            // Forward request to real payment gateway
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Merchant-ID", store.getMerchantId());
            headers.set("X-Signature", signature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending request to gateway...");
            ResponseEntity<Map> gatewayResponse = restTemplate.postForEntity(gatewayUrl, entity, Map.class);

            log.info("Gateway response status: {}", gatewayResponse.getStatusCode());
            log.info("Gateway response body: {}", gatewayResponse.getBody());

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

            // IMPORTANT: Use HARDCODED URL for status check
            String gatewayUrl = HARDCODED_GATEWAY_BASE_URL + "/api/v1/payments/status/" + sessionId;
            log.info("Using HARDCODED gateway URL: {}", gatewayUrl);

            // Prepare headers for signature (if needed)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // If the payment gateway requires authentication for status checks
            if (store.getMerchantId() != null && store.getSecretKey() != null) {
                // Prepare data for signature
                String timestamp = String.valueOf(System.currentTimeMillis());
                String dataToSign = store.getMerchantId() + sessionId + timestamp;

                log.info("Data to sign for status check: {}", dataToSign);

                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(
                        Base64.getDecoder().decode(store.getSecretKey()), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
                String signature = Base64.getEncoder().encodeToString(signatureBytes);

                headers.set("X-Merchant-ID", store.getMerchantId());
                headers.set("X-Timestamp", timestamp);
                headers.set("X-Signature", signature);

                log.info("Added authentication headers for status check");
            }

            HttpEntity<?> entity = new HttpEntity<>(headers);

            try {
                log.info("Sending status check request...");
                ResponseEntity<Map> gatewayResponse = restTemplate.exchange(
                        gatewayUrl,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                log.info("Gateway response status: {}", gatewayResponse.getStatusCode());
                log.info("Gateway response body: {}", gatewayResponse.getBody());

                if (gatewayResponse.getStatusCode().is2xxSuccessful() && gatewayResponse.getBody() != null) {
                    Map<String, Object> responseBody = gatewayResponse.getBody();

                    // Add debugging info
                    responseBody.put("_checkedAt", new Date().toString());
                    responseBody.put("_gatewayUrl", gatewayUrl);

                    return ResponseEntity.ok(responseBody);
                } else {
                    log.warn("Gateway returned non-success status: {}", gatewayResponse.getStatusCode());
                    return ResponseEntity.status(gatewayResponse.getStatusCode())
                            .body(Map.of(
                                    "error", "Gateway returned error",
                                    "status", "ERROR",
                                    "gatewayStatus", gatewayResponse.getStatusCodeValue()
                            ));
                }

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("HTTP error checking status: {}", e.getStatusCode(), e);
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of(
                                "error", "Gateway error: " + e.getStatusText(),
                                "status", "ERROR",
                                "details", e.getResponseBodyAsString()
                        ));
            } catch (ResourceAccessException e) {
                log.error("Connection error checking status: ", e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "error", "Cannot connect to payment gateway",
                                "status", "ERROR",
                                "details", e.getMessage()
                        ));
            }

        } catch (Exception e) {
            log.error("Internal error checking payment status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error: " + e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }
}
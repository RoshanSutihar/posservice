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

        String signature = null; // Declare signature here so it's accessible

        try {
            // 1. Get store configuration
            log.info("1. Fetching store configuration from database...");
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> {
                        log.error("Store configuration missing from database");
                        return new RuntimeException("Store configuration missing");
                    });

            log.info("Store configuration loaded:");
            log.info("- Merchant ID: {}", store.getMerchantId());
            log.info("- Terminal ID: {}", store.getTerminalId());
            log.info("- Secret Key exists: {}", store.getSecretKey() != null);
            log.info("- Secret Key length: {}", store.getSecretKey() != null ? store.getSecretKey().length() : 0);

            // 2. Validate required fields
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

            // 3. Prepare data for signature
            log.info("2. Preparing data for signature...");
            Map<String, Object> ordered = new LinkedHashMap<>();
            ordered.put("terminalId", requestBody.get("terminalId"));
            ordered.put("amount", requestBody.get("amount"));
            ordered.put("transactionRef", requestBody.get("transactionRef"));
            ordered.put("callbackUrl", requestBody.get("callbackUrl"));

            log.info("Ordered map for signature: {}", ordered);

            String jsonString = new ObjectMapper().writeValueAsString(ordered);
            String dataToSign = store.getMerchantId() + jsonString;

            log.info("JSON String: {}", jsonString);
            log.info("Data to sign: {}", dataToSign);

            // 4. Generate HMAC-SHA256 signature
            log.info("3. Generating HMAC-SHA256 signature...");
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(
                        Base64.getDecoder().decode(store.getSecretKey()), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
                signature = Base64.getEncoder().encodeToString(signatureBytes);

                log.info("Generated signature: {}", signature);
                log.info("Signature length: {}", signature.length());

            } catch (IllegalArgumentException e) {
                log.error("Invalid Base64 secret key: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid secret key format"));
            } catch (Exception e) {
                log.error("Error generating signature: ", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Error generating signature: " + e.getMessage()));
            }

            // Check if signature was generated successfully
            if (signature == null) {
                log.error("Signature generation failed - signature is null");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Signature generation failed"));
            }

            // 5. Prepare request to payment gateway
            log.info("4. Preparing request to payment gateway...");
            String gatewayUrl = HARDCODED_GATEWAY_BASE_URL + "/api/v1/payments/initiate";
            log.info("Using HARDCODED gateway URL: {}", gatewayUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Merchant-ID", store.getMerchantId());
            headers.set("X-Signature", signature);

            log.info("Request headers:");
            log.info("- Content-Type: application/json");
            log.info("- X-Merchant-ID: {}", store.getMerchantId());
            log.info("- X-Signature: [{} chars]", signature.length());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.info("Request entity prepared. Body: {}", requestBody);

            // 6. Send request to gateway
            log.info("5. Sending request to gateway...");
            try {
                ResponseEntity<Map> gatewayResponse = restTemplate.postForEntity(gatewayUrl, entity, Map.class);

                log.info("Gateway response received:");
                log.info("- Status Code: {}", gatewayResponse.getStatusCode());
                log.info("- Headers: {}", gatewayResponse.getHeaders());
                log.info("- Body: {}", gatewayResponse.getBody());

                if (gatewayResponse.getBody() == null) {
                    log.warn("Gateway returned null body");
                    return ResponseEntity.status(gatewayResponse.getStatusCode())
                            .body(Map.of("success", false, "message", "Empty response from payment gateway"));
                }

                return ResponseEntity.status(gatewayResponse.getStatusCode())
                        .body(gatewayResponse.getBody());

            } catch (HttpClientErrorException e) {
                log.error("HTTP Client Error (4xx): {}", e.getStatusCode());
                log.error("Response body: {}", e.getResponseBodyAsString());
                log.error("Error details: ", e);

                try {
                    // Try to parse error response
                    Map errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
                    return ResponseEntity.status(e.getStatusCode())
                            .body(errorResponse);
                } catch (Exception ex) {
                    return ResponseEntity.status(e.getStatusCode())
                            .body(Map.of(
                                    "success", false,
                                    "message", "Payment gateway error: " + e.getStatusText(),
                                    "details", e.getResponseBodyAsString()
                            ));
                }

            } catch (HttpServerErrorException e) {
                log.error("HTTP Server Error (5xx): {}", e.getStatusCode());
                log.error("Response body: {}", e.getResponseBodyAsString());
                log.error("Error details: ", e);

                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of(
                                "success", false,
                                "message", "Payment gateway server error",
                                "details", e.getResponseBodyAsString()
                        ));

            } catch (ResourceAccessException e) {
                log.error("Network/Connection Error: ", e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "success", false,
                                "message", "Cannot connect to payment gateway",
                                "details", e.getMessage()
                        ));

            } catch (Exception e) {
                log.error("Unexpected error calling gateway: ", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "Unexpected error: " + e.getMessage()
                        ));
            }

        } catch (Exception e) {
            log.error("General error in initiate-qr: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to initiate payment: " + e.getMessage()
                    ));
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
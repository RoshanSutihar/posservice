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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    private final StoreInfoRepository storeInfoRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public PaymentFrontendController(StoreInfoRepository storeInfoRepository, RestTemplate restTemplate) {
        this.storeInfoRepository = storeInfoRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/initiate-qr")
    public ResponseEntity<?> initiateQrPayment(@RequestBody Map<String, Object> requestBody) {
        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("Store configuration missing"));

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

            // Forward request to real payment gateway
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Merchant-ID", store.getMerchantId());
            headers.set("X-Signature", signature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String gatewayUrl = store.getApiUrl() + "/api/v1/payments/initiate";
            ResponseEntity<Map> gatewayResponse = restTemplate.postForEntity(gatewayUrl, entity, Map.class);

            return ResponseEntity.status(gatewayResponse.getStatusCode())
                    .body(gatewayResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String sessionId) {
        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("Store configuration missing"));

            String gatewayUrl = store.getApiUrl() + "/api/v1/payments/status/" + sessionId;

            // Prepare headers for signature (if needed)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // If the payment gateway requires authentication for status checks
            if (store.getMerchantId() != null && store.getSecretKey() != null) {
                // Prepare data for signature
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

            try {
                ResponseEntity<Map> gatewayResponse = restTemplate.exchange(
                        gatewayUrl,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                if (gatewayResponse.getStatusCode().is2xxSuccessful() && gatewayResponse.getBody() != null) {
                    Map<String, Object> responseBody = gatewayResponse.getBody();

                    // Add some debugging info if needed
                    responseBody.put("_checkedAt", new Date().toString());
                    responseBody.put("_gatewayUrl", gatewayUrl);

                    return ResponseEntity.ok(responseBody);
                } else {
                    return ResponseEntity.status(gatewayResponse.getStatusCode())
                            .body(Map.of(
                                    "error", "Gateway returned error",
                                    "status", "ERROR",
                                    "gatewayStatus", gatewayResponse.getStatusCodeValue()
                            ));
                }

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                // Handle HTTP errors
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of(
                                "error", "Gateway error: " + e.getStatusText(),
                                "status", "ERROR",
                                "details", e.getResponseBodyAsString()
                        ));
            } catch (ResourceAccessException e) {
                // Handle connection errors
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "error", "Cannot connect to payment gateway",
                                "status", "ERROR",
                                "details", e.getMessage()
                        ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error: " + e.getMessage(),
                            "status", "ERROR"
                    ));
        }
    }


    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        try {
            StoreInfo store = storeInfoRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("Store configuration missing"));

            Map<String, Object> config = new HashMap<>();
            config.put("merchantId", store.getMerchantId());
            config.put("apiUrl", store.getApiUrl());
            config.put("terminalId", "DEFAULT_TERMINAL"); // You might want to store this separately

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load configuration: " + e.getMessage()));
        }
    }
}
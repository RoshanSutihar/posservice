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

@RestController
@RequestMapping("/api/payments")
public class PaymentFrontendController {

    private final StoreInfoRepository storeInfoRepository;
    private final RestTemplate restTemplate; // or use WebClient for reactive

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
}

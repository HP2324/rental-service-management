package com.rental.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PayPalPaymentService {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a PayPal order and returns the order ID and approval URL.
     *
     * @param amount   payment amount
     * @param currency ISO 4217 currency code
     * @return map containing "orderId" and "approvalUrl"
     */
    public Map<String, String> createOrder(BigDecimal amount, String currency) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of(
                                "currency_code", currency.toUpperCase(),
                                "value", amount.toPlainString()
                        )
                )),
                "application_context", Map.of(
                        "return_url", "http://localhost:8080/api/payments/paypal/capture",
                        "cancel_url", "http://localhost:8080/api/payments/paypal/cancel"
                )
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v2/checkout/orders",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        String orderId = (String) responseBody.get("id");

        // Find the approval URL from the links array
        List<Map<String, String>> links = (List<Map<String, String>>) responseBody.get("links");
        String approvalUrl = links.stream()
                .filter(link -> "approve".equals(link.get("rel")))
                .map(link -> link.get("href"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PayPal approval URL not found"));

        log.info("Created PayPal order: {}", orderId);
        return Map.of("orderId", orderId, "approvalUrl", approvalUrl);
    }

    /**
     * Captures an approved PayPal order to complete the payment.
     *
     * @param orderId the PayPal order ID returned from createOrder
     * @return capture status ("COMPLETED" on success)
     */
    public String captureOrder(String orderId) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v2/checkout/orders/" + orderId + "/capture",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        String status = (String) responseBody.get("status");
        log.info("Captured PayPal order {}: status={}", orderId, status);
        return status;
    }

    /**
     * Fetches a short-lived OAuth2 access token from PayPal.
     */
    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v1/oauth2/token",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }
}

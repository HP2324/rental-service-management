package com.rental.payment.controller;

import com.rental.payment.dto.PaymentRequest;
import com.rental.payment.dto.PaymentResponse;
import com.rental.payment.service.PaymentService;
import com.rental.shared.dto.ApiResponse;
import com.rental.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments  [TENANT only — initiates Stripe or PayPal payment]
    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.initiatePayment(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", payment));
    }

    // POST /api/payments/paypal/capture?orderId=xxx  [called after PayPal approval redirect]
    @PostMapping("/paypal/capture")
    public ResponseEntity<ApiResponse<PaymentResponse>> capturePayPalPayment(
            @RequestParam String orderId) {
        PaymentResponse payment = paymentService.capturePayPalPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment captured", payment));
    }

    // GET /api/payments/{id}  [owner or ADMIN]
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getById(id)));
    }

    // GET /api/payments/my-payments  [TENANT — payments they made]
    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getMyPayments(user.userId())));
    }

    // GET /api/payments/received  [LANDLORD — payments received]
    @GetMapping("/received")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getReceivedPayments(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getReceivedPayments(user.userId())));
    }

    // POST /api/payments/{id}/sync-status  [any authenticated user]
    // Fetches latest status from Stripe and updates the DB record.
    // Call this after confirming a PaymentIntent via Stripe.js or the Stripe API directly.
    @PostMapping("/{id}/sync-status")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncStripeStatus(@PathVariable String id) {
        PaymentResponse payment = paymentService.syncStripeStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status synced", payment));
    }
}

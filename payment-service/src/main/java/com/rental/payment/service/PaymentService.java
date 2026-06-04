package com.rental.payment.service;

import com.rental.payment.dto.PaymentRequest;
import com.rental.payment.dto.PaymentResponse;
import com.rental.payment.model.Payment;
import com.rental.payment.model.PaymentProvider;
import com.rental.payment.model.PaymentStatus;
import com.rental.payment.repository.PaymentRepository;
import com.rental.shared.exception.ResourceNotFoundException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;
    private final PayPalPaymentService payPalPaymentService;

    /**
     * Initiates a payment via Stripe or PayPal.
     * Returns a PaymentResponse with provider-specific fields:
     * - Stripe: clientSecret (for frontend to confirm)
     * - PayPal:  approvalUrl (redirect user here)
     */
    public PaymentResponse initiatePayment(String tenantId, PaymentRequest request) {
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .landlordId(request.getLandlordId())
                .listingId(request.getListingId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .provider(request.getProvider())
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        String clientSecret = null;
        String approvalUrl = null;

        try {
            if (request.getProvider() == PaymentProvider.STRIPE) {
                PaymentIntent intent = stripePaymentService.createPaymentIntent(
                        request.getAmount(), request.getCurrency(), request.getDescription());
                payment.setProviderPaymentId(intent.getId());
                payment.setStatus(PaymentStatus.PROCESSING);
                clientSecret = intent.getClientSecret();

            } else if (request.getProvider() == PaymentProvider.PAYPAL) {
                Map<String, String> orderInfo = payPalPaymentService.createOrder(
                        request.getAmount(), request.getCurrency());
                payment.setProviderPaymentId(orderInfo.get("orderId"));
                payment.setStatus(PaymentStatus.PROCESSING);
                approvalUrl = orderInfo.get("approvalUrl");
            }
        } catch (Exception e) {
            log.error("Payment initiation failed", e);
            payment.setStatus(PaymentStatus.FAILED);
        }

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved, clientSecret, approvalUrl);
    }

    /**
     * Captures a PayPal order after the user approves it.
     */
    public PaymentResponse capturePayPalPayment(String orderId) {
        Payment payment = paymentRepository.findByProviderPaymentId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        try {
            String status = payPalPaymentService.captureOrder(orderId);
            payment.setStatus("COMPLETED".equals(status) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        } catch (Exception e) {
            log.error("PayPal capture failed for order {}", orderId, e);
            payment.setStatus(PaymentStatus.FAILED);
        }

        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment), null, null);
    }

    public PaymentResponse getById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        return toResponse(payment, null, null);
    }

    public List<PaymentResponse> getMyPayments(String tenantId) {
        return paymentRepository.findByTenantId(tenantId).stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getReceivedPayments(String landlordId) {
        return paymentRepository.findByLandlordId(landlordId).stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    /**
     * Fetches the latest status from Stripe and syncs it to the DB.
     * Call this after confirming a PaymentIntent via the Stripe API or Stripe.js.
     */
    public PaymentResponse syncStripeStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getProvider() != PaymentProvider.STRIPE || payment.getProviderPaymentId() == null) {
            throw new IllegalArgumentException("Payment is not a Stripe payment or has no provider ID");
        }

        try {
            PaymentIntent intent = stripePaymentService.retrievePaymentIntent(payment.getProviderPaymentId());
            payment.setStatus(mapStripeStatus(intent.getStatus()));
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Synced Stripe status for payment {}: {}", paymentId, intent.getStatus());
        } catch (Exception e) {
            log.error("Failed to sync Stripe status for payment {}", paymentId, e);
            throw new RuntimeException("Failed to sync Stripe status: " + e.getMessage());
        }

        return toResponse(payment, null, null);
    }

    private PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded"        -> PaymentStatus.COMPLETED;
            case "processing"       -> PaymentStatus.PROCESSING;
            case "requires_payment_method",
                 "requires_confirmation",
                 "requires_action"  -> PaymentStatus.PENDING;
            case "canceled"         -> PaymentStatus.CANCELLED;
            default                 -> PaymentStatus.FAILED;
        };
    }

    private PaymentResponse toResponse(Payment payment, String clientSecret, String approvalUrl) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .landlordId(payment.getLandlordId())
                .listingId(payment.getListingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .providerPaymentId(payment.getProviderPaymentId())
                .clientSecret(clientSecret)
                .approvalUrl(approvalUrl)
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

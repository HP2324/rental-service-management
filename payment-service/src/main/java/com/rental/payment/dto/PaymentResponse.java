package com.rental.payment.dto;

import com.rental.payment.model.PaymentProvider;
import com.rental.payment.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String tenantId;
    private String landlordId;
    private String listingId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentStatus status;
    private String providerPaymentId;
    // Client secret for Stripe (used by frontend to confirm payment)
    private String clientSecret;
    // Approval URL for PayPal (redirect user here)
    private String approvalUrl;
    private String description;
    private LocalDateTime createdAt;
}

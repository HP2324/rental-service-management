package com.rental.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String landlordId;

    private String listingId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentStatus status;

    // Stripe PaymentIntent ID or PayPal order ID
    @Indexed(unique = true, sparse = true)
    private String providerPaymentId;

    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

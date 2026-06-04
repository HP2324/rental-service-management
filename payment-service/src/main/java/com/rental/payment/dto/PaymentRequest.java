package com.rental.payment.dto;

import com.rental.payment.model.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotBlank(message = "Listing ID is required")
    private String listingId;

    @NotBlank(message = "Landlord ID is required")
    private String landlordId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "USD";

    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    private String description;
}

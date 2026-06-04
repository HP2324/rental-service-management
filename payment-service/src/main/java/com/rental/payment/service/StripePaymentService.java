package com.rental.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripePaymentService {

    /**
     * Creates a Stripe PaymentIntent.
     *
     * @param amount   payment amount in the smallest currency unit (e.g. cents for USD)
     * @param currency ISO 4217 currency code (e.g. "usd")
     * @param description human-readable payment description
     * @return the created PaymentIntent
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String description)
            throws StripeException {

        // Stripe requires the amount in the smallest currency unit (cents)
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                // automatic_payment_methods lets Stripe decide the best payment method
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("Created Stripe PaymentIntent: {}", intent.getId());
        return intent;
    }

    /**
     * Retrieves a PaymentIntent by ID to check its status.
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }
}

package com.rental.payment.repository;

import com.rental.payment.model.Payment;
import com.rental.payment.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByTenantId(String tenantId);
    List<Payment> findByLandlordId(String landlordId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}

package com.ecommerce.commerce.domain.exception;

import java.util.Map;

public class PaymentReferenceConflictException extends DomainException {

    public PaymentReferenceConflictException(String paymentMethod, String providerReference) {
        super("Payment reference is already assigned to another order", Map.of(
                "paymentMethod", paymentMethod,
                "providerReference", providerReference
        ));
    }
}

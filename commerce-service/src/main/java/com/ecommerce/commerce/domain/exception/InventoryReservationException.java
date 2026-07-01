package com.ecommerce.commerce.domain.exception;

import java.util.Map;

public class InventoryReservationException extends DomainException {

    public InventoryReservationException(String message, Map<String, Object> details) {
        super(message, details);
    }
}

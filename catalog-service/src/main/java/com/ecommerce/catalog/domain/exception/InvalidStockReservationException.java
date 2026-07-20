package com.ecommerce.catalog.domain.exception;

import java.util.Map;

public class InvalidStockReservationException extends DomainException {

    public InvalidStockReservationException(String message, Map<String, Object> details) {
        super(message, details);
    }
}

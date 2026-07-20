package com.ecommerce.commerce.domain.exception;

import java.util.Map;

public class CatalogUnavailableException extends DomainException {

    public CatalogUnavailableException(Throwable cause) {
        super("Catalog service is temporarily unavailable", Map.of("service", "catalog"));
        initCause(cause);
    }
}

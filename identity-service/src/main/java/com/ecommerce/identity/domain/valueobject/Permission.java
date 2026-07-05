package com.ecommerce.identity.domain.valueobject;

public enum Permission {
    CATALOG_READ("catalog:read"),
    CATALOG_WRITE("catalog:write"),
    STOCK_MANAGE("stock:manage"),
    CART_MANAGE("cart:manage"),
    CHECKOUT_CREATE("checkout:create"),
    ORDERS_READ_SELF("orders:read:self"),
    ORDERS_PAY_SELF("orders:pay:self"),
    ORDERS_CANCEL_SELF("orders:cancel:self"),
    USERS_READ_SELF("users:read:self"),
    USERS_MANAGE("users:manage");

    private final String scope;

    Permission(String scope) {
        this.scope = scope;
    }

    public String scope() {
        return scope;
    }
}

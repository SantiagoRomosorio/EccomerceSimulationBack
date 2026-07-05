package com.ecommerce.identity.domain.valueobject;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    CUSTOMER(EnumSet.of(
            Permission.CATALOG_READ,
            Permission.CART_MANAGE,
            Permission.CHECKOUT_CREATE,
            Permission.ORDERS_READ_SELF,
            Permission.ORDERS_PAY_SELF,
            Permission.ORDERS_CANCEL_SELF,
            Permission.USERS_READ_SELF
    )),
    ADMIN(EnumSet.allOf(Permission.class));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public Set<Permission> permissions() {
        return permissions;
    }
}

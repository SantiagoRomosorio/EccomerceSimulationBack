package com.ecommerce.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void customerHasOnlySelfServicePermissions() {
        assertThat(UserRole.CUSTOMER.permissions())
                .containsExactlyInAnyOrder(
                        Permission.CATALOG_READ,
                        Permission.CART_MANAGE,
                        Permission.CHECKOUT_CREATE,
                        Permission.ORDERS_READ_SELF,
                        Permission.ORDERS_PAY_SELF,
                        Permission.ORDERS_CANCEL_SELF,
                        Permission.USERS_READ_SELF
                )
                .doesNotContain(
                        Permission.CATALOG_WRITE,
                        Permission.STOCK_MANAGE,
                        Permission.USERS_MANAGE
                );
    }

    @Test
    void adminHasEveryDefinedPermission() {
        assertThat(UserRole.ADMIN.permissions())
                .containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void permissionScopesAreStableContractValues() {
        assertThat(Permission.CATALOG_READ.scope()).isEqualTo("catalog:read");
        assertThat(Permission.USERS_MANAGE.scope()).isEqualTo("users:manage");
    }
}

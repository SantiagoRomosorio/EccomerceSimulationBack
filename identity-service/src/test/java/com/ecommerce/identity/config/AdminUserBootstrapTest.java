package com.ecommerce.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.identity.application.port.out.LoadUserPort;
import com.ecommerce.identity.application.port.out.SaveUserPort;
import com.ecommerce.identity.config.properties.AdminBootstrapProperties;
import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.domain.valueobject.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminUserBootstrapTest {

    private final EncodingPasswordEncoder passwordEncoder = new EncodingPasswordEncoder();

    @Test
    void doesNothingWhenAdminCredentialsAreNotConfigured() {
        FakeUserPorts userPorts = new FakeUserPorts(false);
        AdminUserBootstrap bootstrap = bootstrap(new AdminBootstrapProperties(null, null, null), userPorts);

        bootstrap.run(null);

        assertThat(userPorts.savedUser).isNull();
        assertThat(userPorts.existsByEmailCalls).isZero();
    }

    @Test
    void createsAdminWhenCredentialsAreConfiguredAndUserDoesNotExist() {
        FakeUserPorts userPorts = new FakeUserPorts(false);
        AdminUserBootstrap bootstrap = bootstrap(new AdminBootstrapProperties(
                " Admin@Example.com ",
                "Password123",
                " Root Admin "
        ), userPorts);

        bootstrap.run(null);

        assertThat(userPorts.existsByEmailCalls).isEqualTo(1);
        assertThat(userPorts.savedUser).isNotNull();
        assertThat(userPorts.savedUser.email()).isEqualTo("admin@example.com");
        assertThat(userPorts.savedUser.fullName()).isEqualTo("Root Admin");
        assertThat(userPorts.savedUser.passwordHash()).isEqualTo("encoded:Password123");
        assertThat(userPorts.savedUser.roles()).containsExactlyInAnyOrder(UserRole.ADMIN);
        assertThat(userPorts.savedUser.active()).isTrue();
    }

    @Test
    void skipsAdminCreationWhenUserAlreadyExists() {
        FakeUserPorts userPorts = new FakeUserPorts(true);
        AdminUserBootstrap bootstrap = bootstrap(new AdminBootstrapProperties(
                "admin@example.com",
                "Password123",
                "Admin"
        ), userPorts);

        bootstrap.run(null);

        assertThat(userPorts.existsByEmailCalls).isEqualTo(1);
        assertThat(userPorts.savedUser).isNull();
    }

    @Test
    void failsFastWhenAdminCredentialsAreIncomplete() {
        FakeUserPorts userPorts = new FakeUserPorts(false);
        AdminUserBootstrap bootstrap = bootstrap(new AdminBootstrapProperties(
                "admin@example.com",
                null,
                "Admin"
        ), userPorts);

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ADMIN_EMAIL and ADMIN_PASSWORD must be configured together");
    }

    private AdminUserBootstrap bootstrap(AdminBootstrapProperties properties, FakeUserPorts userPorts) {
        return new AdminUserBootstrap(properties, userPorts, userPorts, passwordEncoder);
    }

    private static class FakeUserPorts implements LoadUserPort, SaveUserPort {

        private final boolean userExists;
        private int existsByEmailCalls;
        private User savedUser;

        private FakeUserPorts(boolean userExists) {
            this.userExists = userExists;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public boolean existsByEmail(String email) {
            existsByEmailCalls++;
            return userExists;
        }

        @Override
        public User save(User user) {
            savedUser = user;
            return user;
        }
    }

    private static class EncodingPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals(encode(rawPassword));
        }
    }
}

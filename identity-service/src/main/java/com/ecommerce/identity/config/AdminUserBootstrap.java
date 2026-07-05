package com.ecommerce.identity.config;

import com.ecommerce.identity.application.port.out.LoadUserPort;
import com.ecommerce.identity.application.port.out.SaveUserPort;
import com.ecommerce.identity.config.properties.AdminBootstrapProperties;
import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.domain.valueobject.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrap.class);

    private final AdminBootstrapProperties properties;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoder passwordEncoder;

    public AdminUserBootstrap(
            AdminBootstrapProperties properties,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.incomplete()) {
            throw new IllegalStateException("ADMIN_EMAIL and ADMIN_PASSWORD must be configured together");
        }

        if (!properties.configured()) {
            return;
        }

        String email = properties.normalizedEmail();
        if (loadUserPort.existsByEmail(email)) {
            log.info("Admin bootstrap skipped because user already exists: {}", email);
            return;
        }

        User admin = new User(
                UUID.randomUUID(),
                email,
                properties.resolvedFullName(),
                passwordEncoder.encode(properties.password()),
                Set.of(UserRole.ADMIN),
                true,
                Instant.now()
        );

        saveUserPort.save(admin);
        log.info("Admin bootstrap user created: {}", email);
    }
}

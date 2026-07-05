package com.ecommerce.identity.application.service;

import com.ecommerce.identity.application.port.in.GetCurrentUserUseCase;
import com.ecommerce.identity.application.port.in.LoginUseCase;
import com.ecommerce.identity.application.port.in.RegisterUserUseCase;
import com.ecommerce.identity.application.port.out.GenerateTokenPort;
import com.ecommerce.identity.application.port.out.LoadUserPort;
import com.ecommerce.identity.application.port.out.SaveUserPort;
import com.ecommerce.identity.config.properties.JwtProperties;
import com.ecommerce.identity.domain.exception.ConflictException;
import com.ecommerce.identity.domain.exception.DomainErrorCode;
import com.ecommerce.identity.domain.exception.DomainException;
import com.ecommerce.identity.domain.exception.InvalidCredentialsException;
import com.ecommerce.identity.domain.exception.ResourceNotFoundException;
import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.domain.valueobject.Permission;
import com.ecommerce.identity.domain.valueobject.UserRole;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService implements RegisterUserUseCase, LoginUseCase, GetCurrentUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final GenerateTokenPort generateTokenPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public IdentityService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            GenerateTokenPort generateTokenPort,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties
    ) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.generateTokenPort = generateTokenPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public User register(RegisterUserUseCase.Command command) {
        String email = normalizeEmail(command.email());

        if (loadUserPort.existsByEmail(email)) {
            throw new ConflictException(
                    DomainErrorCode.EMAIL_ALREADY_REGISTERED,
                    Map.of("email", email)
            );
        }

        User user = new User(
                UUID.randomUUID(),
                email,
                command.fullName(),
                passwordEncoder.encode(command.password()),
                Set.of(UserRole.CUSTOMER),
                true,
                Instant.now()
        );

        return saveUserPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginUseCase.Result login(LoginUseCase.Command command) {
        User user = loadUserPort.findByEmail(normalizeEmail(command.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.active() || !passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = generateTokenPort.generate(
                user.id().toString(),
                Map.of(
                        "email", user.email(),
                        "roles", user.roles().stream().map(Enum::name).collect(Collectors.toSet()),
                        "scope", resolveScopeClaim(user.roles())
                )
        );

        return new LoginUseCase.Result(token, "Bearer", jwtProperties.expirationMinutes());
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return loadUserPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        DomainErrorCode.USER_NOT_FOUND,
                        Map.of("userId", userId)
                ));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException(
                    DomainErrorCode.VALIDATION_ERROR,
                    "Email is required",
                    Map.of("field", "email")
            );
        }

        return email.trim().toLowerCase();
    }

    private String resolveScopeClaim(Set<UserRole> roles) {
        return roles.stream()
                .flatMap(role -> role.permissions().stream())
                .map(Permission::scope)
                .distinct()
                .sorted()
                .collect(Collectors.joining(" "));
    }
}

package com.ecommerce.identity.domain.model;

import com.ecommerce.identity.domain.exception.DomainException;
import com.ecommerce.identity.domain.valueobject.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Set<UserRole> roles;
    private final boolean active;
    private final Instant createdAt;

    public User(
            UUID id,
            String email,
            String fullName,
            String passwordHash,
            Set<UserRole> roles,
            boolean active,
            Instant createdAt
    ) {
        if (email == null || email.isBlank()) {
            throw new DomainException("User email is required");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("User full name is required");
        }

        this.id = id;
        this.email = email.trim().toLowerCase();
        this.fullName = fullName.trim();
        this.passwordHash = passwordHash;
        this.roles = roles == null || roles.isEmpty() ? Set.of(UserRole.CUSTOMER) : Set.copyOf(roles);
        this.active = active;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<UserRole> roles() {
        return roles;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

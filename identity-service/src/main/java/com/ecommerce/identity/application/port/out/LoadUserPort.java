package com.ecommerce.identity.application.port.out;

import com.ecommerce.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}

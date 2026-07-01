package com.ecommerce.identity.application.port.in;

import com.ecommerce.identity.domain.model.User;

public interface RegisterUserUseCase {

    User register(Command command);

    record Command(
            String email,
            String fullName,
            String password
    ) {
    }
}

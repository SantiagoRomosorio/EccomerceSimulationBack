package com.ecommerce.identity.application.port.in;

public interface LoginUseCase {

    Result login(Command command);

    record Command(
            String email,
            String password
    ) {
    }

    record Result(
            String accessToken,
            String tokenType,
            long expiresInMinutes
    ) {
    }
}

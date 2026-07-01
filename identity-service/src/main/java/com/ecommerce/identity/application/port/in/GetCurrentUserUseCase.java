package com.ecommerce.identity.application.port.in;

import com.ecommerce.identity.domain.model.User;
import java.util.UUID;

public interface GetCurrentUserUseCase {

    User getById(UUID userId);
}

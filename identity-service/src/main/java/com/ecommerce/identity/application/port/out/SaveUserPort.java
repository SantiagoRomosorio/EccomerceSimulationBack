package com.ecommerce.identity.application.port.out;

import com.ecommerce.identity.domain.model.User;

public interface SaveUserPort {

    User save(User user);
}

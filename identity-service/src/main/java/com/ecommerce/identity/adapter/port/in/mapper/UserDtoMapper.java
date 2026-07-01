package com.ecommerce.identity.adapter.port.in.mapper;

import com.ecommerce.identity.adapter.port.in.dto.UserResponse;
import com.ecommerce.identity.domain.model.User;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.fullName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.active(),
                user.createdAt()
        );
    }
}

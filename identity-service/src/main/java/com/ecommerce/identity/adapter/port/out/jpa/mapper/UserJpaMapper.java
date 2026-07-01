package com.ecommerce.identity.adapter.port.out.jpa.mapper;

import com.ecommerce.identity.adapter.port.out.jpa.entity.UserEntity;
import com.ecommerce.identity.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserJpaMapper {

    public User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getRoles(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setFullName(user.fullName());
        entity.setPasswordHash(user.passwordHash());
        entity.setRoles(user.roles());
        entity.setActive(user.active());
        entity.setCreatedAt(user.createdAt());
        return entity;
    }
}

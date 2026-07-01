package com.ecommerce.identity.adapter.port.out.jpa;

import com.ecommerce.identity.adapter.port.out.jpa.mapper.UserJpaMapper;
import com.ecommerce.identity.adapter.port.out.jpa.repository.UserJpaRepository;
import com.ecommerce.identity.application.port.out.LoadUserPort;
import com.ecommerce.identity.application.port.out.SaveUserPort;
import com.ecommerce.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(userJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return userJpaMapper.toDomain(userJpaRepository.save(userJpaMapper.toEntity(user)));
    }
}

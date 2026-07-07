package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.in.CreateUserUseCase;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepository;

    public CreateUserService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User create(Command command) {
        if (userRepository.existsByUsername(command.username())) {
            throw duplicateUsername(command.username());
        }
        try {
            return userRepository.save(User.create(command.username(), command.displayName()));
        } catch (DataIntegrityViolationException e) {
            // 사전 검사와 INSERT 사이의 경합은 DB UNIQUE 제약이 최종 방어한다
            throw duplicateUsername(command.username());
        }
    }

    private ApiException duplicateUsername(String username) {
        return ApiException.conflict("DUPLICATE_USERNAME", "이미 사용 중인 username입니다: " + username);
    }
}

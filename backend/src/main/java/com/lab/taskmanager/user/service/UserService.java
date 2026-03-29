package com.lab.taskmanager.user.service;

import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Load the current user entity by username for downstream task ownership checks.
     *
     * @param username username from the authenticated principal
     * @return resolved user entity
     */
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("当前用户不存在"));
    }
}

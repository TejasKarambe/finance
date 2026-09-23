
package com.example.finance.service;

import com.example.finance.dto.request.UserRequest;
import com.example.finance.dto.response.UserResponse;
import com.example.finance.entity.User;
import com.example.finance.exception.UserAlreadyExistsException;
import com.example.finance.exception.UserNotFoundException;
import com.example.finance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserRequest request) {

        // checks if a user with the same email already exists in the database
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "User with this email already exists");
        }

        // If no user with the same email is found, a new User entity is created
        User user = new User(
                request.name(),
                request.email(),
                LocalDateTime.now());

        // The new user is saved to the database using userRepository.save()
        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    public List<UserResponse> getUsers() {

        return userRepository.findAll()
                .stream() // 1. Creates a stream from the list of users
                .map(this::mapToResponse) // 2. converting each user entity to user response DTO using mapper method
                .toList(); // 3. collecting the converted user response DTO objects into a list
    }

    public UserResponse getUserByID(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));

        return mapToResponse(user);
    }

    // mapper method: helps to convert entity to response DTO object
    // Reduces code duplication and improves readability
    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt());
    }
}
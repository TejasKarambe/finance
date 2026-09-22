package com.example.finance.service;

import com.example.finance.dto.request.UserRequest;
import com.example.finance.dto.response.UserResponse;
import com.example.finance.entity.User;
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

//    public User createUser(String name, String email) {
//
//        User user = new User(
//                name,
//                email,
//                LocalDateTime.now()
//        );
//
//        return userRepository.save(user);
//    }
    public UserResponse createUser(UserRequest request) {

        User user = new User(
                request.name(),
                request.email(),
                LocalDateTime.now()
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUserByID(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User Not Found"
                        )
                        );
    }
}
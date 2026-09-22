package com.example.finance.controller;

import com.example.finance.dto.request.UserRequest;
import com.example.finance.dto.response.UserResponse;
import com.example.finance.entity.User;
import com.example.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // @PostMapping
    // public User createUser(
    // @RequestParam String name,
    // @RequestParam String email
    // ) {
    // return userService.createUser(name, email);
    // }

    // @valid: it uses for applying validations defined in user request dto
    @PostMapping
    public UserResponse createUser(
            @Valid @RequestBody UserRequest request) {
        return userService.createUser(request);

    }

    @GetMapping
    public List<User> getAllUser() {
        return userService.getUsers();
    }

    @GetMapping("/{id}")
    public User getUserByID(@PathVariable Long id) {
        return userService.getUserByID(id);
    }
}

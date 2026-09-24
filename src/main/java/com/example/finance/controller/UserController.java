package com.example.finance.controller;

import com.example.finance.dto.request.UserRequest;
import com.example.finance.dto.request.UserUpdateRequest;
import com.example.finance.dto.response.UserResponse;
import com.example.finance.service.UserService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(
            @Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> getAllUser() {
        return userService.getUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserByID(@PathVariable Long id) {
        return userService.getUserByID(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id); // calling the delete method from service layer

        return ResponseEntity.noContent().build(); // 204 - NO CONTENT
    }

}
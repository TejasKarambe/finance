package com.example.finance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(

                @NotBlank(message = "Name is required") String name,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email

) {
}
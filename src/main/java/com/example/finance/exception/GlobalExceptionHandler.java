package com.example.finance.exception;

import com.example.finance.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice  //centralized exception handler for all controllers in the application
public class GlobalExceptionHandler {

        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(
                        UserNotFoundException ex) {

                ErrorResponse error = new ErrorResponse(
                                404,            // Not Found
                                ex.getMessage(),
                                LocalDateTime.now());

                return ResponseEntity  //HTTP response that contains the error information
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

}

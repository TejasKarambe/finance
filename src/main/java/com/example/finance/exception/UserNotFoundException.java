package com.example.finance.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);  //calls the constructor of the parent class (RuntimeException) which is RuntimeException
    }

}

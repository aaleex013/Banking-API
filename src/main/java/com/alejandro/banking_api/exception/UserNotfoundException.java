package com.alejandro.banking_api.exception;

public class UserNotfoundException extends RuntimeException {
    public UserNotfoundException(String message) {
        super(message);
    }
}

package com.ahorra.common.exception;

public class ActiveSessionExistsException extends RuntimeException {
    public ActiveSessionExistsException(String message) {
        super(message);
    }
}
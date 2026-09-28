package com.ahorra.common.exception;

public class CustomApiException extends RuntimeException {

    public CustomApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.ahorra.common.exception;

import lombok.Getter;

@Getter
public class AlreadyExistException extends RuntimeException {

    public AlreadyExistException(String message) {
        super(message);
    }
}

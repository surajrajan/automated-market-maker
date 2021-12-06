package com.model.exception;

import lombok.experimental.StandardException;

@StandardException
public class InvalidInputException extends Exception {
    public InvalidInputException() {
        this(null, null);
    }

    public InvalidInputException(String message) {
        this(message, null);
    }

    public InvalidInputException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message);
        if (cause != null) super.initCause(cause);
    }
}

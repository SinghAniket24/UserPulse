package com.aniket.userpulse;

public class InvalidActivityEventException extends RuntimeException {

    public InvalidActivityEventException(String message) {
        super(message);
    }

    public InvalidActivityEventException(String message, Throwable cause) {
        super(message, cause);
    }
}

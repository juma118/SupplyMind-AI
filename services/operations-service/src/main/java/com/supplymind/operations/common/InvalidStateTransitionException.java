package com.supplymind.operations.common;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}

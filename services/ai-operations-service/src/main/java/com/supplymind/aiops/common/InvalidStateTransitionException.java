package com.supplymind.aiops.common;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}

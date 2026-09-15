package com.campusiq.exception;

public class StudentProfileAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StudentProfileAlreadyExistsException(String message) {
        super(message);
    }
}

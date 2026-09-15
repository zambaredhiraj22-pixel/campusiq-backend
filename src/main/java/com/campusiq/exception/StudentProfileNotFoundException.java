package com.campusiq.exception;

public class StudentProfileNotFoundException extends RuntimeException {

    public StudentProfileNotFoundException(String message) {
        super(message);
    }
}
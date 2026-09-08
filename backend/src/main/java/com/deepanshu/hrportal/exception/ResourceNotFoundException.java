package com.deepanshu.hrportal.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String r, String f, Object v) {
        super(r + " not found with " + f + " = '" + v + "'");
    }
}

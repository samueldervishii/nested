package com.nested.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}

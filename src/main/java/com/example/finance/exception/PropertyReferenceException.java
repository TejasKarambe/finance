package com.example.finance.exception;

public class PropertyReferenceException
        extends RuntimeException {

    private final String propertyName;

    public PropertyReferenceException(String message, String propertyName) {
        super(message);
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
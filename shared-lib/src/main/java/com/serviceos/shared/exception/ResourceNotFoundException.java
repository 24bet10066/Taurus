package com.serviceos.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final Object identifier;

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s not found: %s".formatted(resource, identifier));
        this.resource = resource;
        this.identifier = identifier;
    }

    public String getResource() {
        return resource;
    }

    public Object getIdentifier() {
        return identifier;
    }
}

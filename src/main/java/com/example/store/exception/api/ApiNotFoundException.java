package com.example.store.exception.api;

import lombok.Getter;

@Getter
public class ApiNotFoundException extends RuntimeException {

    private Long id;
    private String resourceType;

    public ApiNotFoundException(Long id, String resourceType) {
        super(String.format("The requested resource of type %s was not found for ID %s", resourceType, id));
        this.id = id;
        this.resourceType = resourceType;
    }
}

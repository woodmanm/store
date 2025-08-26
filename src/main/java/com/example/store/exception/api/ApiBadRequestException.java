package com.example.store.exception.api;

import lombok.Getter;

@Getter
public class ApiBadRequestException extends RuntimeException {

    private String messageKey;

    public ApiBadRequestException(String message, String messageKey) {
        super(message);
        this.messageKey = messageKey;
    }
}

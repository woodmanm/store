package com.example.store.exception.api;

import jakarta.persistence.PersistenceException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

@ControllerAdvice
@SuppressWarnings("unused")
public class StoreExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<Object> handleApiNotFoundException(ApiNotFoundException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
        problemDetail.setTitle(HttpStatus.NOT_FOUND.name());
        String message = getMessageSource()
                .getMessage(
                        "api.resource.not.found", new Object[] {ex.getResourceType(), ex.getId()}, Locale.getDefault());
        /*
          If the application allowed for users to select their locale then this would be sourced from either
          the user's record or from a request scoped bean.
        */
        problemDetail.setDetail(message);
        return handleExceptionInternal(
                ex, problemDetail, null, HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), request);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Object> handlePersistenceException(PersistenceException ex, WebRequest request) {
        logger.error(ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.name());
        String message = getMessageSource().getMessage("api.internal.server.error", new Object[0], Locale.getDefault());
        problemDetail.setDetail(message);
        return handleExceptionInternal(
                ex,
                problemDetail,
                HttpHeaders.EMPTY,
                HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                request);
    }
}

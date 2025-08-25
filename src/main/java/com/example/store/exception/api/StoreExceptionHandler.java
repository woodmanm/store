package com.example.store.exception.api;

import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
@SuppressWarnings("unused")
@Primary
public class StoreExceptionHandler extends ResponseEntityExceptionHandler {

    public StoreExceptionHandler(MessageSource messageSource) {
        setMessageSource(messageSource);
    }

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<Object> handleApiNotFoundException(ApiNotFoundException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
        problemDetail.setTitle(HttpStatus.NOT_FOUND.getReasonPhrase());
        String message = getMessageSource()
                .getMessage(
                        "api.resource.not.found", new Object[] {ex.getResourceType(), ex.getId()}, Locale.getDefault());
        /*
          If the application allowed for customers to select their locale then this would be sourced from either
          the customer's record or from a request scoped bean.
        */
        problemDetail.setDetail(message);
        return handleExceptionInternal(
                ex, problemDetail, HttpHeaders.EMPTY, HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), request);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Object> handlePersistenceException(PersistenceException ex, WebRequest request) {
        logger.error(ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        String message = getMessageSource().getMessage("api.internal.server.error", new Object[0], Locale.getDefault());
        problemDetail.setDetail(message);
        return handleExceptionInternal(
                ex,
                problemDetail,
                HttpHeaders.EMPTY,
                HttpStatusCode.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        logger.error(ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        String message = getMessageSource().getMessage("api.bad.request", new Object[0], Locale.getDefault());
        problemDetail.setDetail(message);
        final Map<String, Set<String>> failures = new HashMap<>();
        ex.getConstraintViolations().forEach(e -> {
            String field = e.getPropertyPath().toString();
            // could tidy up this field value.
            if (!failures.containsKey(field)) {
                failures.put(field, new HashSet<>());
            }
            failures.get(field).add(e.getMessage());
        });
        problemDetail.setProperty("failures", failures);
        return handleExceptionInternal(
                ex, problemDetail, HttpHeaders.EMPTY, HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), request);
    }

    /*
       Something is off here. It shouldn't be this much work to resolve the custom message from the key.
    */
    @Nullable @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        final Map<String, Set<String>> failures = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            if (e instanceof FieldError fieldError) {
                String field = fieldError.getField();
                if (!failures.containsKey(fieldError.getField())) {
                    failures.put(field, new HashSet<>());
                }
                failures.get(field).add(fieldError.getDefaultMessage());
            }
        });
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setDetail("Invalid input");
        problemDetail.setProperty("failures", failures);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }
}

package com.example.store.exception.api;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreExceptionHandlerTest {

    private MessageSource messageSource;
    private WebRequest webRequest = mock(WebRequest.class);
    private StoreExceptionHandler classUnderTest;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        classUnderTest = new StoreExceptionHandler(messageSource);
    }

    @Test
    void thatApiNotFoundExceptionIsHandled() {
        when(messageSource.getMessage("api.resource.not.found", new Object[] {"Order", 1L}, Locale.getDefault()))
                .thenReturn("The requested resource of type Order was not found for ID 1");

        ResponseEntity<Object> response =
                classUnderTest.handleApiNotFoundException(new ApiNotFoundException(1L, "Order"), webRequest);

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getStatus(), is(404));
        assertThat(problemDetail.getType().toASCIIString(), is("about:blank"));
        assertThat(problemDetail.getTitle(), is("Not Found"));
        assertThat(problemDetail.getDetail(), is("The requested resource of type Order was not found for ID 1"));
    }

    @Test
    void thatPersistenceExceptionIsHandled() {
        when(messageSource.getMessage("api.internal.server.error", new Object[0], Locale.getDefault()))
                .thenReturn("An error has occurred - Please try again in a few minutes");

        ResponseEntity<Object> response =
                classUnderTest.handlePersistenceException(new PersistenceException("DB error"), webRequest);

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getStatus(), is(500));
        assertThat(problemDetail.getType().toASCIIString(), is("about:blank"));
        assertThat(problemDetail.getTitle(), is("Internal Server Error"));
        assertThat(problemDetail.getDetail(), is("An error has occurred - Please try again in a few minutes"));
    }

    @Test
    void thatHandleMethodArgumentNotValidIsCorrect() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors())
                .thenReturn(List.of(
                        new FieldError("1", "name", "first error"),
                        new FieldError("2", "name", "second error"),
                        new FieldError("3", "direction", "south")));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<Object> response = classUnderTest.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatusCode.valueOf(400), mock(WebRequest.class));

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getStatus(), is(400));
        assertThat(problemDetail.getTitle(), is("Bad Request"));
        assertThat(problemDetail.getDetail(), is("Invalid input"));
        Map<String, Object> properties = problemDetail.getProperties();
        assertThat(properties.size(), is(1));
        Map<String, Set<String>> failures = (Map<String, Set<String>>) properties.get("failures");
        assertThat(failures.get("name"), is(Set.of("second error", "first error")));
        assertThat(failures.get("direction"), is(Set.of("south")));
    }
}

package com.example.store.exception.api;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;

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
        classUnderTest = new StoreExceptionHandler();
        classUnderTest.setMessageSource(messageSource);
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
        assertThat(problemDetail.getTitle(), is("NOT_FOUND"));
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
        assertThat(problemDetail.getTitle(), is("INTERNAL_SERVER_ERROR"));
        assertThat(problemDetail.getDetail(), is("An error has occurred - Please try again in a few minutes"));
    }
}

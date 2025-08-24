package com.example.store.integration.customer;

import com.example.store.integration.AbstractIntegrationTestBase;
import com.example.store.presentation.CustomerSearchRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CustomerSearchIntegrationTest extends AbstractIntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"John Smith", "john", "SMITH", "oh", "hn sm"})
    void thatSearchUserByNameReturnsTheExpectedResult(String name) {
        CustomerSearchRequest request = new CustomerSearchRequest();
        request.setName(name);
        given().body(request)
                .post("/customer/search")
                .then()
                .statusCode(200)
                .body("name", hasItems("John Smith"))
                .body("$.size()", is(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"John Mith", "mith"})
    void thatMultipleResultsAreReturned(String name) {
        CustomerSearchRequest request = new CustomerSearchRequest();
        request.setName(name);
        given().body(request)
                .post("/customer/search")
                .then()
                .statusCode(200)
                .body("$.size()", is(2))
                .body("find() {it.name == 'John Smith'}.id", notNullValue())
                .body("find() {it.name == 'John Smith'}.orders", hasItems())
                .body("find() {it.name == 'Jack Mitheral'}.id", notNullValue())
                .body("find() {it.name == 'Jack Mitheral'}.orders", hasItems());
    }

    @Test
    void thatEmptyResultIsReturned() {
        CustomerSearchRequest request = new CustomerSearchRequest();
        request.setName("Baba Yaga");
        given().body(request).post("/customer/search").then().statusCode(200).body("$.size()", is(0));
    }

    /*
       Note here that there could be tests for invalid inputs but that is over the top for these integration tests.
    */
}

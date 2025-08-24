package com.example.store.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CustomerSearchRequest {

    @Size(min = 2, max = 255, message = "{customer.search.input}")
    @JsonProperty("name")
    private String name;
}

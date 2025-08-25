package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDTO {

    @NotNull @Positive private Long id;

    @NotBlank
    @Size(min = 2, max = 255)
    private String description;

    private List<OrderDTO> orders = new ArrayList<>();
}

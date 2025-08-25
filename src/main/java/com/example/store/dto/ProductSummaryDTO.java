package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSummaryDTO {

    @NotNull
    @Positive
    private Long id;

    @NotBlank
    @Size(min = 2, max = 255)
    private String description;

    @Builder.Default
    private List<Long> orders = new ArrayList<>();
}

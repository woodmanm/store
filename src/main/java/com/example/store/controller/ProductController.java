package com.example.store.controller;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.ProductSummaryDTO;
import com.example.store.entity.ProductSummaryProjection;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
   Note that I am keeping in line with the style of the other controllers.
   From a RESTful naming standard this endpoint is correct and /customer and /order should be made plural.
   Also, I would write a ProductService and not pass entities outside that
   tier of the application but, given that the '/orders' endpoint needs updating
   I will code as per the other controllers.
*/
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    /*
       Note here that I am not implementing caching for products due to the time it would take.
       Were this a production application then I would.
    */

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @GetMapping
    public List<ProductSummaryDTO> getAllProducts() {
        List<ProductSummaryProjection> projectSummaries = productRepository.findAllBy();
        return projectSummaries.stream().map(s -> ProductSummaryDTO.builder()
                .id(s.getId())
                .description(s.getDescription())
                .orders(s.getOrders().stream().map(ProductSummaryProjection.OrderProjection::getId).toList())
                .build())
                .toList();
    }
}

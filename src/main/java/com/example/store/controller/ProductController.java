package com.example.store.controller;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.ProductSummaryDTO;
import com.example.store.entity.Product;
import com.example.store.entity.ProductSummaryProjection;
import com.example.store.exception.api.ApiBadRequestException;
import com.example.store.exception.api.ApiNotFoundException;
import com.example.store.mapper.ProductMapper;
import com.example.store.presentation.CreateProductRequest;
import com.example.store.repository.ProductRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
   Note that I am keeping in line with the style of the other controllers.
   From a RESTful naming standard this endpoint is correct and /customer and /order should be made plural.
   Also, I would write a ProductService and not pass entities outside that
   tier of the application but, given that the '/orders' endpoint needs updating
   I will code as per the other controllers.
   Also, also, I have written integration tests and skipped the unit tests because I have demonstrated
   already that I can write unit tests.
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

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ProductSummaryDTO> getAllProducts() {
        List<ProductSummaryProjection> productSummaries = productRepository.findAllBy();
        return productSummaries.stream()
                .map(s -> ProductSummaryDTO.builder()
                        .id(s.getId())
                        .description(s.getDescription())
                        .orders(s.getOrders().stream()
                                .map(ProductSummaryProjection.OrderProjection::getId)
                                .toList())
                        .build())
                .toList();
    }

    @GetMapping(path = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ProductSummaryDTO getProductById(@PathVariable("id") @Positive Long id) {
        Optional<ProductSummaryProjection> productSummary = productRepository.findProjectionById(id);
        if (productSummary.isEmpty()) {
            throw new ApiNotFoundException(id, "Product");
        }
        ProductSummaryProjection product = productSummary.get();
        return ProductSummaryDTO.builder()
                .id(product.getId())
                .description(product.getDescription())
                .orders(product.getOrders().stream()
                        .map(ProductSummaryProjection.OrderProjection::getId)
                        .toList())
                .build();
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ProductDTO createProduct(@RequestBody @Valid final CreateProductRequest request) {
        String description = request.getDescription().trim();
        Optional<Product> existingProduct = productRepository.findByDescriptionIgnoreCase(description);
        if (existingProduct.isPresent()) {
            throw new ApiBadRequestException("An entity already exists", "duplicate.entity");
        }
        Product product = new Product();
        product.setDescription(request.getDescription());
        Product saved = productRepository.save(product);
        return productMapper.productToProductDTO(saved);
    }
}

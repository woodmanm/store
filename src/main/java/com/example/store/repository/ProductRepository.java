package com.example.store.repository;

import com.example.store.entity.Product;
import com.example.store.entity.ProductSummaryProjection;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {

    List<ProductSummaryProjection> findAllBy();

    Optional<Product> findByDescriptionIgnoreCase(String description);

    Optional<ProductSummaryProjection> findProjectionById(Long id);
}

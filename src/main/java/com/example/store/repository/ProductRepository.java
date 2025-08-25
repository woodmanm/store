package com.example.store.repository;

import com.example.store.entity.Product;
import com.example.store.entity.ProductSummaryProjection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ListCrudRepository<Product, Long> {

    List<ProductSummaryProjection> findAllBy();
}

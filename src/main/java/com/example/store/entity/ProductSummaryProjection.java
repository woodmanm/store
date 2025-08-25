package com.example.store.entity;

import java.util.List;

public interface ProductSummaryProjection {

    Long getId();

    String getDescription();

    List<OrderProjection> getOrders();

    interface OrderProjection {
        Long getId();
    }
}

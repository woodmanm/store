package com.example.store.integration;

public interface InTransactionSupplier<T> {
    T execute();
}

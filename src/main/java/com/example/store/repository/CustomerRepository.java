package com.example.store.repository;

import com.example.store.entity.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    /*
        This is only used for test setup and should be moved to a test component.
        I will leave this as is because Spring is managing the implementation so no test is required.
    */
    Optional<Customer> findByName(String name);
}

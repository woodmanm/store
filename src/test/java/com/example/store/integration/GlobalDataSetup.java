package com.example.store.integration;

import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.repository.CustomerRepository;

import org.springframework.boot.test.context.TestComponent;

import java.util.ArrayList;

@TestComponent
public class GlobalDataSetup {

    private final CustomerRepository customerRepository;

    public GlobalDataSetup(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public void createCustomers() {
        if (customerRepository.findByName("John Smith").isEmpty()) {
            Customer johnSmith = new Customer();
            johnSmith.setName("John Smith");
            johnSmith.setOrders(new ArrayList<Order>());
            customerRepository.save(johnSmith);
        }
        if (customerRepository.findByName("Jack Mitheral").isEmpty()) {
            Customer jackMitheral = new Customer();
            jackMitheral.setName("Jack Mitheral");
            jackMitheral.setOrders(new ArrayList<Order>());
            customerRepository.save(jackMitheral);
        }
    }
}

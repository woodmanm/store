package com.example.store.controller;

import com.example.store.configuration.CacheConfiguration;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.exception.api.ApiNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CacheManager cacheManager;
    private final CustomerRepository customerRepository;

    @GetMapping
    @Cacheable(value = CacheConfiguration.ALL_ORDERS)
    public List<OrderDTO> getAllOrders() {
        return orderMapper.ordersToOrderDTOs(orderRepository.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfiguration.ALL_ORDERS, allEntries = true),
                @CacheEvict(value = CacheConfiguration.ALL_CUSTOMERS, allEntries = true),
                @CacheEvict(value = CacheConfiguration.CUSTOMERS, key = "#order.customer.id")
            })
    public OrderDTO createOrder(@RequestBody Order order) {
        Long customerId = order.getCustomer().getId();
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty()) {
            throw new ApiNotFoundException(customerId, "Customer");
        }
        Customer customerEntity = customer.get();
        order.setCustomer(customerEntity);
        Order entity = orderRepository.save(order);
        cacheManager.getCache(CacheConfiguration.ORDERS).putIfAbsent(entity.getId(), entity);
        return orderMapper.orderToOrderDTO(entity);
    }

    /*
    Nice to have. 'io.swagger' to keep the spec in sync with the code
    (unless you are writing the spec first and generating the code from that).
    */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Cacheable(value = "orders", key = "#id")
    public OrderDTO getOrderById(@PathVariable(name = "id") @Positive Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            throw new ApiNotFoundException(id, "Order");
        }
        return orderMapper.orderToOrderDTO(order.get());
    }
}

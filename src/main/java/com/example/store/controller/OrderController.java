package com.example.store.controller;

import com.example.store.configuration.CacheConfiguration;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.api.ApiBadRequestException;
import com.example.store.exception.api.ApiNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CacheManager cacheManager;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

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
                @CacheEvict(value = CacheConfiguration.CUSTOMERS, allEntries = true)
            })
    public OrderDTO createOrder(@RequestBody @Valid final Order order) {
        Long customerId = order.getCustomer().getId();
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty()) {
            throw new ApiNotFoundException(customerId, "Customer");
        }
        Customer customerEntity = customerOptional.get();
        /*
        Copying the order. Allowing the caller to send an entity with an ID will lead to all sorts of issues.
        The request objects should have all been model object with the appropriate fields.
        For example, in the real world a product would also be associated with a quantity so
        the model for this endpoint would need only, description, customer.id and products[n].id with
        products[n].quantity.
        */
        final Order orderEntity = new Order();
        orderEntity.setCustomer(customerEntity);
        customerEntity.getOrders().add(orderEntity);
        orderEntity.setDescription(order.getDescription());
        if (CollectionUtils.isEmpty(order.getProducts())) {
            // If we were using a model object then this could be annotated on the request object
            throw new ApiBadRequestException("At least one product is required", "order.product.size");
        }
        order.getProducts().forEach(p -> {
            Optional<Product> productOptional = productRepository.findById(p.getId());
            if (productOptional.isEmpty()) {
                throw new ApiNotFoundException(p.getId(), "Product");
            }
            Product product = productOptional.get();
            orderEntity.getProducts().add(product);
            product.getOrders().add(orderEntity);
        });
        Order savedOrder = orderRepository.save(orderEntity);
        OrderDTO orderDTO = orderMapper.orderToOrderDTO(savedOrder);
        try {
            cacheManager.getCache(CacheConfiguration.ORDERS).putIfAbsent(orderDTO.getId(), orderDTO);
        } catch (RuntimeException ex) {
            // If the caching isn't working we don't want the application to fail.
            // TODO log this
        }
        return orderDTO;
    }

    /*
    Nice to have. 'io.swagger' to keep the spec in sync with the code
    (unless you are writing the spec first and generating the code from that).
    */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Cacheable(value = CacheConfiguration.ORDERS)
    public OrderDTO getOrderById(@PathVariable(name = "id") @Positive Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            throw new ApiNotFoundException(id, "Order");
        }
        return orderMapper.orderToOrderDTO(order.get());
    }
}

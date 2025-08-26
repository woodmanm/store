package com.example.store.controller;

import com.example.store.configuration.CacheConfiguration;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.QueryTimeoutException;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ComponentScan(basePackageClasses = {CustomerMapper.class, CacheConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
@RequiredArgsConstructor
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private ProductRepository productRepository;

    private Order order;
    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);

        order = new Order();
        order.setDescription("Test Order");
        order.setId(1L);
        order.setCustomer(customer);

        product1 = new Product();
        product1.setDescription("Test Product 1");
        product1.setId(2L);

        product2 = new Product();
        product2.setDescription("Test Product 2");
        product2.setId(3L);

        order.getProducts().add(product1);
        order.getProducts().add(product2);

        when(productRepository.findById(2L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product2));

        cacheManager
                .getCacheNames()
                .forEach(cache -> cacheManager.getCache(cache).clear());
    }

    @Test
    void testCreateOrder() throws Exception {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(a -> {
            Order order = (Order) a.getArguments()[0];
            if (order.getId() == null) {
                order.setId(5L);
            }
            return order;
        });

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"))
                .andExpect(jsonPath("$.products[0].id").value(2L))
                .andExpect(jsonPath("$.products[0].description").value("Test Product 1"))
                .andExpect(jsonPath("$.products[1].id").value(3L))
                .andExpect(jsonPath("$.products[1].description").value("Test Product 2"));
    }

    @Test
    void testGetOrder() throws Exception {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].description").value("Test Order"))
                .andExpect(jsonPath("$.[0].customer.name").value("John Doe"))
                .andExpect(jsonPath("$.[0].products[0].description").value("Test Product 1"))
                .andExpect(jsonPath("$.[0].products[0].id").value(2L))
                .andExpect(jsonPath("$.[0].products[1].description").value("Test Product 2"))
                .andExpect(jsonPath("$.[0].products[1].id").value(3L));
    }

    @Test
    void thatFindOrderByIdReturnsCorrectly() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"))
                .andExpect(jsonPath("$.customer.id").value(1L));
    }

    @Test
    void thatFindOrderByIdReturnsNotFound() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("The requested resource of type Order was not found for ID  1"))
                .andExpect(jsonPath("$.instance").value("/order/1"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void thatFindOrderByIdForNonPositiveIdReturnsBadRequest(int id) throws Exception {
        mockMvc.perform(get("/order/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid data was supplied"))
                .andExpect(jsonPath("$.instance").value("/order/" + id))
                .andExpect(jsonPath("$.failures.size()").value(1));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void thatFindOrderByIdReturnsInternalErrorWhenPersistenceException(CapturedOutput capturedOutput) throws Exception {
        QueryTimeoutException exception = new QueryTimeoutException("Test Timeout", null);
        exception.setStackTrace(new StackTraceElement[0]);
        when(orderRepository.findById(1L)).thenThrow(exception);

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An error has occurred - Please try again in a few minutes"))
                .andExpect(jsonPath("$.instance").value("/order/1"));

        Assertions.assertTrue(isEventLogged(capturedOutput, QueryTimeoutException.class.getName() + ": Test Timeout"));
    }

    private boolean isEventLogged(CapturedOutput capturedOutput, String value) {
        return Arrays.stream(capturedOutput.getAll().split("\n"))
                .map(String::trim)
                .anyMatch(s -> value.equals(s));
    }
}

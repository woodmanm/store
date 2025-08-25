package com.example.store.controller;

import com.example.store.configuration.CacheConfiguration;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.presentation.CustomerSearchRequest;
import com.example.store.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@ComponentScan(basePackageClasses = {CustomerMapper.class, CacheConfiguration.class})
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);
        cacheManager
                .getCacheNames()
                .forEach(cache -> cacheManager.getCache(cache).clear());
    }

    @Test
    void testCreateCustomer() throws Exception {
        when(customerRepository.save(customer)).thenReturn(customer);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void testGetAllCustomers() throws Exception {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..name").value("John Doe"));
        ;
    }

    @Test
    void testSearchCustomerCallsTheRepositoryCorrectly() throws Exception {
        when(customerRepository.findAll(any(Specification.class))).thenReturn(List.of(customer));
        CustomerSearchRequest request = new CustomerSearchRequest();
        request.setName("John");

        mockMvc.perform(post("/customer/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..name").value("John Doe"));

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(customerRepository).findAll(captor.capture());
        MatcherAssert.assertThat(captor.getValue(), CoreMatchers.notNullValue());
    }
}

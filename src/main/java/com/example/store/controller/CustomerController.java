package com.example.store.controller;

import com.example.store.configuration.CacheConfiguration;
import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.presentation.CustomerSearchRequest;
import com.example.store.repository.CustomerRepository;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CacheManager cacheManager;

    @GetMapping
    @Cacheable(value = CacheConfiguration.ALL_CUSTOMERS)
    public List<CustomerDTO> getAllCustomers() {
        return customerMapper.customersToCustomerDTOs(customerRepository.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    /*
       We need to be very heavy handed with the cache evict because we are passing entities rather than DTOs to the
       create methods in the controllers.
    */
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfiguration.ALL_ORDERS, allEntries = true),
                @CacheEvict(value = CacheConfiguration.ORDERS, allEntries = true),
                @CacheEvict(value = CacheConfiguration.ALL_CUSTOMERS, allEntries = true)
            })
    public CustomerDTO createCustomer(@RequestBody Customer customer) {
        Customer entity = customerRepository.save(customer);
        cacheManager.getCache(CacheConfiguration.CUSTOMERS).putIfAbsent(entity.getId(), entity);
        return customerMapper.customerToCustomerDTO(entity);
    }

    /*
       This could be a GET with query params instead of a POST.
       I have made the assumption that given then input "John Smith" that the result set should contain all records where
       the customer name contains either John or Smith.  Case-insensitive.
    */
    @PostMapping(path = "/search", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<CustomerDTO> findCustomersByName(@RequestBody @Valid CustomerSearchRequest customerSearchRequest) {
        /*
           I would, at this point, consider creating a CustomerService class. A refactor of the current structure is
           OoS for a code challenge.
        */
        List<Customer> foundCustomers =
                customerRepository.findAll(CustomerSpecs.findByNameSpec(customerSearchRequest.getName()));
        return customerMapper.customersToCustomerDTOs(foundCustomers);
    }
}

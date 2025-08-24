package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.presentation.CustomerSearchRequest;
import com.example.store.repository.CustomerRepository;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @GetMapping
    public List<CustomerDTO> getAllCustomers() {
        return customerMapper.customersToCustomerDTOs(customerRepository.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
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

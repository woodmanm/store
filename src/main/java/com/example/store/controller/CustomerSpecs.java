package com.example.store.controller;

import com.example.store.entity.Customer;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;

public class CustomerSpecs {

    public static Specification<Customer> findByNameSpec(String name) {
        String sanitizedName = name == null ? "" : name.replaceAll("\\s", " ");
        final List<String> nameComponents = Arrays.stream(sanitizedName.split(" "))
                .filter(n -> !n.isBlank())
                .distinct()
                .toList();
        /*
           Depending on the requirements we should throw here if the size of this list is maliciously long.
           Like queries are expensive.
        */
        return (root, query, builder) -> {
            List<Predicate> predicates = nameComponents.stream()
                    .map(n -> builder.like(builder.lower(root.get("name")), "%" + n.toLowerCase() + "%"))
                    .toList();
            /*
               Note here that if the requirement is that the result must contain all of the search terms then
               the builder should use 'and' rather than 'or'
            */
            return builder.or(predicates.toArray(new Predicate[0]));
        };
    }
}

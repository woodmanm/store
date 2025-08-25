package com.example.store.integration.product;

import com.example.store.dto.ProductSummaryDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.integration.AbstractIntegrationTestBase;
import com.example.store.presentation.CreateProductRequest;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class ProductIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        runInTransaction(() -> {
            productRepository.deleteAll();
            orderRepository.deleteAll();
            customerRepository.deleteAll();
        });
    }

    @Test
    void thatProductIsCreated() {
        String description = UUID.randomUUID().toString();
        CreateProductRequest request =
                CreateProductRequest.builder().description(description).build();

        long id = given().body(request)
                .when()
                .post("/products")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("description", is(description))
                .body("orders.size()", is(0))
                .extract()
                .jsonPath()
                .getLong("id");

        Optional<Product> product = productRepository.findById(id);
        assertThat(product.isPresent(), is(true));
    }

    @Test
    void thatAllProductsAreFetched() {
        final List<Product> products = new ArrayList<>();
        runInTransaction(() -> {
            for (int n = 0; n < 5; n++) {
                final int index = n;
                Product product = runInTransaction(() -> {
                    Product p = new Product();
                    p.setDescription("Product " + index);
                    return productRepository.save(p);
                });
                products.add(product);
            }
        });

        runInTransaction(() -> {
            Customer customer = new Customer();
            customer.setName("Test Customer");
            customer = customerRepository.save(customer);

            Order myOrder = new Order();
            myOrder.setCustomer(customer);
            myOrder.setDescription("Test order");
            Order savedOrder = orderRepository.save(myOrder);
            customer.getOrders().add(savedOrder);

            products.forEach(p -> savedOrder
                    .getProducts()
                    .add(productRepository.findById(p.getId()).get()));
            savedOrder.getProducts().forEach(p -> p.getOrders().add(savedOrder));
            orderRepository.save(myOrder);
        });

        List<ProductSummaryDTO> result = given().get("/products")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$", ProductSummaryDTO.class);

        assertThat(result.size(), is(5));
        List<String> descriptions =
                result.stream().map(ProductSummaryDTO::getDescription).toList();
        assertThat(descriptions, containsInAnyOrder("Product 0", "Product 1", "Product 2", "Product 3", "Product 4"));
        result.forEach(s -> assertThat(s.getOrders().size(), is(1)));
    }

    @Test
    void thatProductIsFetchedById() {
        Product product = runInTransaction(() -> {
            Product p = new Product();
            p.setDescription("Lookup product");
            return productRepository.save(p);
        });
        runInTransaction(() -> {
            Customer customer = new Customer();
            customer.setName("Test Customer");
            customer = customerRepository.save(customer);

            Order myOrder = new Order();
            myOrder.setCustomer(customer);
            myOrder.setDescription("Test order");
            Order savedOrder = orderRepository.save(myOrder);
            customer.getOrders().add(savedOrder);

            savedOrder
                    .getProducts()
                    .add(productRepository.findById(product.getId()).get());
            savedOrder.getProducts().forEach(p -> p.getOrders().add(savedOrder));
            orderRepository.save(myOrder);
        });

        given().get("/products/{id}", product.getId())
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("description", is("Lookup product"))
                .body("orders.size()", is(1));
    }

    @Test
    void thatDuplicateProductReturnsBadRequest() {
        final String description = "Duplicate product";
        runInTransaction(() -> {
            Product p = new Product();
            p.setDescription(description);
            productRepository.save(p);
        });

        CreateProductRequest request =
                CreateProductRequest.builder().description(description + " ").build();

        given().body(request)
                .when()
                .post("/products")
                .then()
                .statusCode(400)
                .body("title", is("Bad Request"))
                .body("detail", is("The operation would create a duplicate entity"));
    }
}

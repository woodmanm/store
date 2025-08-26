-- Create product table
CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         description VARCHAR(255) NOT NULL
);

-- Create order_product lookup table
CREATE TABLE order_product (
                         order_id BIGINT NOT NULL,
                         product_id BIGINT NOT NULL,
                         CONSTRAINT fk_order_product_order FOREIGN KEY (order_id) REFERENCES "order" (id),
                         CONSTRAINT fk_order_product_product FOREIGN KEY (product_id) REFERENCES product (id)
);
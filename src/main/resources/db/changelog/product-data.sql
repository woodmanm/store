INSERT INTO product (id, description) VALUES (1, 'Default Product');

SELECT setval('product_id_seq', (SELECT max(id) FROM product));

-- add a Product to every order
INSERT INTO order_product (order_id, product_id) SELECT o.id, p.id from "order" o CROSS JOIN product p;
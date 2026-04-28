ALTER TABLE orders
ALTER COLUMN product_id SET NOT NULL;

ALTER TABLE orders
ADD CONSTRAINT unique_user_sale_product
    UNIQUE (user_id, sale_id, product_id);

ALTER TABLE orders
ADD CONSTRAINT fk_orders_product
    FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE order_items
    ADD COLUMN user_id BIGINT NOT NULL,
    ADD COLUMN sale_id BIGINT NOT NULL;

ALTER TABLE order_items
    ADD CONSTRAINT unique_user_sale_product
        UNIQUE (user_id, sale_id, product_id);
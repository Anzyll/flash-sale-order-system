ALTER TABLE orders
DROP CONSTRAINT IF EXISTS unique_user_sale_product;


CREATE UNIQUE INDEX unique_confirmed_order
    ON orders (user_id, sale_id, product_id)
    WHERE status = 'CONFIRMED';
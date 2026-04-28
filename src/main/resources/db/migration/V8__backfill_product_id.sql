UPDATE orders o
SET product_id = oi.product_id
FROM order_items oi
WHERE o.id = oi.order_id;
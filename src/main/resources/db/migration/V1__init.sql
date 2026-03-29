CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE  NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE products(
    id BIGSERIAL PRIMARY KEY ,
    name VARCHAR(255) NOT NULL ,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flash_sales(
    id BIGSERIAL PRIMARY KEY ,
    title VARCHAR(255) NOT NULL ,
    start_time TIMESTAMP  NOT NULL,
    end_time TIMESTAMP NOT NULL ,
    created_at TIMESTAMP DEFAULT  CURRENT_TIMESTAMP
);

CREATE TABLE  sale_items(
    id BIGSERIAL PRIMARY KEY ,
    sale_id BIGINT NOT NULL ,
    product_id BIGINT NOT NULL ,
    sale_price DECIMAL(10,2) NOT NULL ,
    total_stock INT NOT NULL CHECK (total_stock >= 0),
    available_stock INT NOT NULL CHECK (available_stock >= 0),
    CONSTRAINT fk_sale_items_sale FOREIGN KEY(sale_id) REFERENCES flash_sales(id),
    CONSTRAINT  fk_sale_items_products FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT unique_sale_product UNIQUE (product_id,sale_id)
);

CREATE TABLE orders(
    id BIGSERIAL PRIMARY KEY ,
    user_id BIGINT NOT NULL ,
    sale_id BIGINT NOT NULL ,
    status VARCHAR(50) NOT NULL ,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_sale FOREIGN KEY (sale_id) REFERENCES flash_sales(id)
);

CREATE TABLE order_items(
    id BIGSERIAL PRIMARY KEY ,
    order_id BIGINT NOT NULL ,
    product_id BIGINT NOT NULL ,
    quantity INT NOT NULL ,
    price DECIMAL(10,2) NOT NULL ,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);
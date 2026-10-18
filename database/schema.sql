-- =====================================================================
-- Multi-Vendor E-Commerce Platform - PostgreSQL schema
--
-- Optional: the application creates/updates these tables automatically
-- (spring.jpa.hibernate.ddl-auto=update). Run this script instead if you
-- prefer to create the schema yourself, then start the app with DDL_AUTO=none.
--
--   createdb multivendor_db
--   psql -d multivendor_db -f database/schema.sql
-- =====================================================================

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,                      -- BCrypt hash
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'VENDOR', 'CUSTOMER'))
);

CREATE TABLE vendors (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    store_name  VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vendors_user UNIQUE (user_id),
    CONSTRAINT uk_vendors_store_name UNIQUE (store_name),
    CONSTRAINT fk_vendors_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_vendors_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    vendor_id   BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(2000),
    price       NUMERIC(12, 2) NOT NULL,
    stock       INTEGER        NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,        -- false = soft-deleted
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_price CHECK (price > 0),
    CONSTRAINT ck_products_stock CHECK (stock >= 0)
);
CREATE INDEX idx_products_vendor   ON products (vendor_id);
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT  NOT NULL,
    product_id  BIGINT  NOT NULL,
    quantity    INTEGER NOT NULL,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT         NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PLACED',
    total_amount     NUMERIC(14, 2) NOT NULL,
    shipping_address VARCHAR(500)   NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PLACED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_orders_customer ON orders (customer_id);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    vendor_id    BIGINT         NOT NULL,                    -- denormalised: fast "my orders" query per vendor
    product_name VARCHAR(200)   NOT NULL,                    -- snapshot at purchase time
    unit_price   NUMERIC(12, 2) NOT NULL,                    -- snapshot at purchase time
    quantity     INTEGER        NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PLACED',
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_items_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_status CHECK (status IN ('PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);
CREATE INDEX idx_order_items_order  ON order_items (order_id);
CREATE INDEX idx_order_items_vendor ON order_items (vendor_id);

-- ============================================================
--  E-CANTEEN v2  –  Database Schema
--  Database : ecanteen
--  Engine   : MySQL / MariaDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecanteen
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecanteen;

-- ------------------------------------------------------------
-- 1. STALLS  (kantin / lapak vendor)
-- ------------------------------------------------------------
CREATE TABLE stalls (
    id               INT          NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    is_open          TINYINT(1)   NOT NULL DEFAULT 0,
    discount_percent DECIMAL(5,2)          DEFAULT 0.00,
    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- 2. USERS  (student / admin / superadmin)
-- ------------------------------------------------------------
CREATE TABLE users (
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100),
    role          VARCHAR(20)  NOT NULL DEFAULT 'student',
    stall_id      INT                   DEFAULT NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_users_stall FOREIGN KEY (stall_id) REFERENCES stalls (id)
);

-- Akun bawaan sistem
-- superadmin / superadmin123
-- INSERT INTO users (username, password_hash, role) VALUES ('superadmin', '<bcrypt>', 'superadmin');

-- ------------------------------------------------------------
-- 3. MENU_ITEMS  (item makanan / minuman per stall)
-- ------------------------------------------------------------
CREATE TABLE menu_items (
    id           INT            NOT NULL AUTO_INCREMENT,
    stall_id     INT            NOT NULL,
    name         VARCHAR(100)   NOT NULL,
    description  TEXT,
    price        DECIMAL(10,2)  NOT NULL,
    image_url    VARCHAR(255),
    is_available TINYINT(1)     NOT NULL DEFAULT 1,
    stock        INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_stall FOREIGN KEY (stall_id) REFERENCES stalls (id)
);

-- ------------------------------------------------------------
-- 4. CART_ITEMS  (keranjang belanja sementara per user)
-- ------------------------------------------------------------
CREATE TABLE cart_items (
    id           INT NOT NULL AUTO_INCREMENT,
    user_id      INT NOT NULL,
    menu_item_id INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_user      FOREIGN KEY (user_id)      REFERENCES users      (id),
    CONSTRAINT fk_cart_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
);

-- ------------------------------------------------------------
-- 5. ORDERS  (header transaksi; satu order = satu stall)
-- ------------------------------------------------------------
CREATE TABLE orders (
    id              INT           NOT NULL AUTO_INCREMENT,
    user_id         INT           NOT NULL,
    stall_id        INT           NOT NULL,
    order_group_id  INT                    DEFAULT NULL,   -- mengelompokkan multi-stall checkout
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_total  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_method  VARCHAR(30)   NOT NULL DEFAULT 'CASH',
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user  FOREIGN KEY (user_id)  REFERENCES users  (id),
    CONSTRAINT fk_orders_stall FOREIGN KEY (stall_id) REFERENCES stalls (id)
);

-- ------------------------------------------------------------
-- 6. ORDER_ITEMS  (baris detail per order; harga dibekukan saat checkout)
-- ------------------------------------------------------------
CREATE TABLE order_items (
    id           INT           NOT NULL AUTO_INCREMENT,
    order_id     INT           NOT NULL,
    menu_item_id INT                    DEFAULT NULL,   -- NULL jika item sudah dihapus
    item_name    VARCHAR(100)  NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_oi_order     FOREIGN KEY (order_id)     REFERENCES orders     (id),
    CONSTRAINT fk_oi_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
);

-- ------------------------------------------------------------
-- 7. ORDER_STATUS_LOGS  (riwayat perubahan status order)
-- ------------------------------------------------------------
CREATE TABLE order_status_logs (
    id         INT         NOT NULL AUTO_INCREMENT,
    order_id   INT         NOT NULL,
    status     VARCHAR(30) NOT NULL,
    changed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_osl_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- ------------------------------------------------------------
-- 8. DISCOUNTS  (diskon global yang diatur oleh admin)
-- ------------------------------------------------------------
CREATE TABLE discounts (
    id               INT          NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    is_active        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- 9. REVIEWS  (ulasan menu oleh student)
-- ------------------------------------------------------------
CREATE TABLE reviews (
    id           INT      NOT NULL AUTO_INCREMENT,
    menu_item_id INT      NOT NULL,
    user_id      INT      NOT NULL,
    rating       TINYINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_rev_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_rev_user FOREIGN KEY (user_id)      REFERENCES users      (id)
);

-- ------------------------------------------------------------
-- 10. SECURITY_LOGS  (audit trail: login, order, aksi admin)
-- ------------------------------------------------------------
CREATE TABLE security_logs (
    id         INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50),
    action     VARCHAR(50)  NOT NULL,   -- LOGIN_SUCCESS / LOGIN_FAILURE / ORDER_PLACED / dll
    detail     TEXT,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- 11. VENDOR_APPLICATIONS  (pendaftaran vendor baru → disetujui superadmin)
-- ------------------------------------------------------------
CREATE TABLE vendor_applications (
    id                INT         NOT NULL AUTO_INCREMENT,
    user_id           INT                  DEFAULT NULL,
    stall_name        VARCHAR(100) NOT NULL,
    stall_description TEXT,
    owner_name        VARCHAR(100) NOT NULL,
    phone             VARCHAR(20),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING / APPROVED / REJECTED
    reject_reason     TEXT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_va_user FOREIGN KEY (user_id) REFERENCES users (id)
);

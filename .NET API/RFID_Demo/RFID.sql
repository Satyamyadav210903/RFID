Create database RFID_Demo

use RFID_Demo

/*-----------------------------
        USERS TABLE
------------------------------*/
CREATE TABLE users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50),
    role VARCHAR(30),
	password varchar(50)
);

select * from users

/*-----------------------------
        PRODUCTS TABLE
------------------------------*/

CREATE TABLE products (
    product_id INT IDENTITY(1,1) PRIMARY KEY,
    tag_id VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(100),
    category VARCHAR(50),
    size VARCHAR(10),
    color VARCHAR(30)
);

/*-----------------------------
        SCAN LOG TABLE
------------------------------*/
CREATE TABLE scan_logs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    tag_id VARCHAR(50),
    scan_time DATETIME,
    location VARCHAR(50),
    status VARCHAR(20),
    FOREIGN KEY (tag_id) REFERENCES products(tag_id)
);


INSERT INTO products (tag_id, product_name, category, size, color) VALUES
('300833B2DDD9014000000001', 'Cotton T-Shirt', 'T-Shirt', 'M', 'Black'),
('300833B2DDD9014000000002', 'Cotton T-Shirt', 'T-Shirt', 'L', 'White'),
('300833B2DDD9014000000003', 'Denim Jeans', 'Jeans', '32', 'Blue'),
('300833B2DDD9014000000004', 'Denim Jeans', 'Jeans', '34', 'Black'),
('300833B2DDD9014000000005', 'Formal Shirt', 'Shirt', 'M', 'Sky Blue'),
('300833B2DDD9014000000006', 'Formal Shirt', 'Shirt', 'L', 'White'),
('300833B2DDD9014000000007', 'Hoodie', 'Jacket', 'M', 'Grey'),
('300833B2DDD9014000000008', 'Hoodie', 'Jacket', 'L', 'Black'),
('300833B2DDD9014000000009', 'Casual Shorts', 'Shorts', 'M', 'Navy'),
('300833B2DDD9014000000010', 'Track Pants', 'Pants', 'L', 'Dark Grey');


DELETE scan_logs

INSERT INTO scan_logs (tag_id, scan_time, location, status) VALUES
('300833B2DDD9014000000001', GETDATE(), 'Warehouse', 'ACTIVE'),
('300833B2DDD9014000000002', GETDATE(), 'Warehouse', 'DISABLE'),
('300833B2DDD9014000000003', GETDATE(), 'Warehouse', 'DISABLE'),
('300833B2DDD9014000000004', GETDATE(), 'Warehouse', 'DISABLE'),
('300833B2DDD9014000000005', GETDATE(), 'Store-A', 'DISABLE'),
('300833B2DDD9014000000006', GETDATE(), 'Store-A', 'ACTIVE'),
('300833B2DDD9014000000007', GETDATE(), 'Store-B', 'ACTIVE'),
('300833B2DDD9014000000008', GETDATE(), 'Store-B', 'ACTIVE'),
('300833B2DDD9014000000009', GETDATE(), 'Store-C', 'ACTIVE'),
('300833B2DDD9014000000010', GETDATE(), 'Store-C', 'ACTIVE');


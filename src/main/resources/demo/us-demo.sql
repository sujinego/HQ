-- ── 고객 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer (
                                        customer_id   VARCHAR(50)   PRIMARY KEY,
    customer_name VARCHAR(100),
    contact_email VARCHAR(100),
    credit_limit  DECIMAL(12,2),
    current_credit DECIMAL(12,2)
    );
INSERT INTO customer VALUES ('CUST_US_001','Best Buy','bestbuy@example.com',80000000,8000000);
INSERT INTO customer VALUES ('CUST_US_002','Walmart','walmart@example.com',100000000,12000000);
INSERT INTO customer VALUES ('CUST_US_003','Amazon','amazon@example.com',150000000,5000000);
INSERT INTO customer VALUES ('CUST_US_004','Target','target@example.com',50000000,3500000);

-- ── 주문 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
                                      order_no     VARCHAR(50)   PRIMARY KEY,
    customer_id  VARCHAR(50),
    total_amount DECIMAL(12,2),
    status       VARCHAR(20),
    order_date   DATE,
    created_at   DATETIME DEFAULT NOW()
    );
INSERT INTO orders VALUES ('ORD-US-001','CUST_US_001',2598,'COMPLETED','2026-07-01',NOW());
INSERT INTO orders VALUES ('ORD-US-002','CUST_US_002',4995,'CONFIRMED','2026-07-02',NOW());
INSERT INTO orders VALUES ('ORD-US-003','CUST_US_001',1299,'SHIPPING','2026-07-04',NOW());
INSERT INTO orders VALUES ('ORD-US-004','CUST_US_003',6495,'PENDING','2026-07-05',NOW());
INSERT INTO orders VALUES ('ORD-US-005','CUST_US_004',998,'CONFIRMED','2026-07-06',NOW());

-- ── 주문 상세 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
                                           id           INT PRIMARY KEY AUTO_INCREMENT,
                                           order_no     VARCHAR(50),
    product_code VARCHAR(20),
    quantity     INT,
    unit_price   DECIMAL(12,2)
    );
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-001','LG-GRAM-17',2,1299);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-002','LG-MON-27-4K',5,499);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-002','LG-OLED-65',1,2490);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-003','LG-GRAM-17',1,1299);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-004','LG-OLED-65',3,1899);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-004','LG-AC-DUAL',1,798);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-US-005','LG-MON-27-4K',2,499);

-- ── 상품 가격 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS local_product_price (
                                                   product_code VARCHAR(20) PRIMARY KEY,
    product_name VARCHAR(100),
    sale_price   DECIMAL(12,2),
    currency     VARCHAR(10) DEFAULT 'USD',
    synced_at    DATETIME DEFAULT NOW()
    );
INSERT INTO local_product_price VALUES ('LG-GRAM-17','LG Gram 17',1299,'USD',NOW());
INSERT INTO local_product_price VALUES ('LG-MON-27-4K','LG UltraFine 27',499,'USD',NOW());
INSERT INTO local_product_price VALUES ('LG-OLED-65','LG OLED TV 65',1899,'USD',NOW());
INSERT INTO local_product_price VALUES ('LG-AC-DUAL','LG Dual AC',990,'USD',NOW());
INSERT INTO local_product_price VALUES ('LG-WASH-21','LG Trom Washer',840,'USD',NOW());
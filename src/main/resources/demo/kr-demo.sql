-- ── 고객 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer (
                                        customer_id   VARCHAR(50)   PRIMARY KEY,
    customer_name VARCHAR(100),
    contact_email VARCHAR(100),
    credit_limit  DECIMAL(12,2),
    current_credit DECIMAL(12,2)
    );
INSERT INTO customer VALUES ('CUST_KR_001','삼성전자','samsung@example.com',50000000,5000000);
INSERT INTO customer VALUES ('CUST_KR_002','LG전자','lg@example.com',30000000,500000);
INSERT INTO customer VALUES ('CUST_KR_003','현대차','hyundai@example.com',20000000,3200000);
INSERT INTO customer VALUES ('CUST_KR_004','SK하이닉스','sk@example.com',40000000,1800000);
INSERT INTO customer VALUES ('CUST_KR_005','카카오','kakao@example.com',15000000,900000);

-- ── 주문 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
                                      order_no     VARCHAR(50)   PRIMARY KEY,
    customer_id  VARCHAR(50),
    total_amount DECIMAL(12,2),
    status       VARCHAR(20),
    order_date   DATE,
    created_at   DATETIME DEFAULT NOW()
    );
INSERT INTO orders VALUES ('ORD-KR-001','CUST_KR_001',2500000,'COMPLETED','2026-07-01',NOW());
INSERT INTO orders VALUES ('ORD-KR-002','CUST_KR_001',1900000,'CONFIRMED','2026-07-02',NOW());
INSERT INTO orders VALUES ('ORD-KR-003','CUST_KR_002',500000,'CANCELLED','2026-07-03',NOW());
INSERT INTO orders VALUES ('ORD-KR-004','CUST_KR_003',3200000,'PENDING','2026-07-04',NOW());
INSERT INTO orders VALUES ('ORD-KR-005','CUST_KR_001',1800000,'SHIPPING','2026-07-05',NOW());
INSERT INTO orders VALUES ('ORD-KR-006','CUST_KR_004',4500000,'CONFIRMED','2026-07-05',NOW());
INSERT INTO orders VALUES ('ORD-KR-007','CUST_KR_005',890000,'PENDING','2026-07-06',NOW());

-- ── 주문 상세 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
                                           id           INT PRIMARY KEY AUTO_INCREMENT,
                                           order_no     VARCHAR(50),
    product_code VARCHAR(20),
    quantity     INT,
    unit_price   DECIMAL(12,2)
    );
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-001','LG-GRAM-17',1,1690000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-001','LG-MON-27-4K',1,649000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-002','LG-OLED-65',1,1900000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-004','LG-GRAM-17',2,1690000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-005','LG-OLED-65',1,1800000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-006','LG-GRAM-17',2,1690000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-006','LG-AC-DUAL',1,1290000);
INSERT INTO order_items (order_no,product_code,quantity,unit_price) VALUES ('ORD-KR-007','LG-MON-27-4K',1,649000);

-- ── 상품 가격 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS local_product_price (
                                                   product_code VARCHAR(20) PRIMARY KEY,
    product_name VARCHAR(100),
    sale_price   DECIMAL(12,2),
    currency     VARCHAR(10) DEFAULT 'KRW',
    synced_at    DATETIME DEFAULT NOW()
    );
INSERT INTO local_product_price VALUES ('LG-GRAM-17','LG 그램 17인치',1690000,'KRW',NOW());
INSERT INTO local_product_price VALUES ('LG-MON-27-4K','LG 울트라파인 27',649000,'KRW',NOW());
INSERT INTO local_product_price VALUES ('LG-OLED-65','LG OLED TV 65',2490000,'KRW',NOW());
INSERT INTO local_product_price VALUES ('LG-AC-DUAL','LG 듀얼 에어컨',1290000,'KRW',NOW());
INSERT INTO local_product_price VALUES ('LG-WASH-21','LG 트롬 세탁기',1090000,'KRW',NOW());
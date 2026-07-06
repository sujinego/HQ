-- ── 국가 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS country_ent (
                                           country_code VARCHAR(10) PRIMARY KEY,
    country_name VARCHAR(100),
    currency     VARCHAR(10),
    is_active    TINYINT DEFAULT 1
    );
INSERT INTO country_ent VALUES ('KR','한국법인','KRW',1);
INSERT INTO country_ent VALUES ('US','미국법인','USD',1);

-- ── 상품 마스터 ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_master (
                                              product_code    VARCHAR(20)   PRIMARY KEY,
    product_name_ko VARCHAR(100)  NOT NULL,
    product_name_en VARCHAR(100),
    category        VARCHAR(50),
    base_cost       DECIMAL(12,2),
    sale_price_krw  DECIMAL(12,2),
    sale_price_usd  DECIMAL(12,2),
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at      DATETIME      DEFAULT NOW(),
    updated_at      DATETIME      DEFAULT NOW()
    );
INSERT INTO product_master VALUES ('LG-GRAM-17','LG 그램 17인치','LG Gram 17','NOTEBOOK',1200000,1690000,1299,'ACTIVE',NOW(),NOW());
INSERT INTO product_master VALUES ('LG-MON-27-4K','LG 울트라파인 27','LG UltraFine 27','MONITOR',450000,649000,499,'ACTIVE',NOW(),NOW());
INSERT INTO product_master VALUES ('LG-OLED-65','LG OLED TV 65','LG OLED TV 65','TV',1800000,2490000,1899,'ACTIVE',NOW(),NOW());
INSERT INTO product_master VALUES ('LG-AC-DUAL','LG 듀얼 에어컨','LG Dual AC','AC',900000,1290000,990,'ACTIVE',NOW(),NOW());
INSERT INTO product_master VALUES ('LG-WASH-21','LG 트롬 세탁기','LG Trom Washer','APPLIANCE',700000,1090000,840,'ACTIVE',NOW(),NOW());

-- ── 환율 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exchange_rate (
                                             id            INT PRIMARY KEY AUTO_INCREMENT,
                                             rate_date     DATE,
                                             currency_code VARCHAR(10),
    rate_to_krw   DECIMAL(10,2),
    created_at    DATETIME DEFAULT NOW()
    );
INSERT INTO exchange_rate (rate_date,currency_code,rate_to_krw) VALUES (CURDATE(),'USD',1320.00);
INSERT INTO exchange_rate (rate_date,currency_code,rate_to_krw) VALUES (CURDATE(),'EUR',1430.00);
INSERT INTO exchange_rate (rate_date,currency_code,rate_to_krw) VALUES (CURDATE(),'JPY',8.85);
INSERT INTO exchange_rate (rate_date,currency_code,rate_to_krw) VALUES (CURDATE(),'CNY',182.00);
INSERT INTO exchange_rate (rate_date,currency_code,rate_to_krw) VALUES (CURDATE(),'GBP',1670.00);

-- ── 매출 집계 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fact_sales (
                                          id            INT PRIMARY KEY AUTO_INCREMENT,
                                          country_code  VARCHAR(10),
    product_code  VARCHAR(20),
    sale_date     DATE,
    quantity      INT,
    amount_local  DECIMAL(12,2),
    currency_code VARCHAR(10),
    amount_krw    DECIMAL(12,2),
    created_at    DATETIME DEFAULT NOW()
    );
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('KR','LG-GRAM-17',CURDATE(),3,5070000,'KRW',5070000);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('KR','LG-OLED-65',CURDATE(),2,4980000,'KRW',4980000);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('KR','LG-MON-27-4K',CURDATE(),5,3245000,'KRW',3245000);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('US','LG-MON-27-4K',CURDATE(),5,2495,'USD',3293400);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('US','LG-GRAM-17',CURDATE(),2,2598,'USD',3429360);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('KR','LG-GRAM-17',DATEADD(MONTH,-1,CURDATE()),10,16900000,'KRW',16900000);
INSERT INTO fact_sales (country_code,product_code,sale_date,quantity,amount_local,currency_code,amount_krw) VALUES ('US','LG-OLED-65',DATEADD(MONTH,-1,CURDATE()),8,15192,'USD',20053440);

-- ── 포털 사용자 ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS portal_user (
                                           id       INT PRIMARY KEY AUTO_INCREMENT,
                                           username VARCHAR(50),
    password VARCHAR(200),
    role     VARCHAR(50),
    enabled  INT DEFAULT 1  -- ← 추가
    );

INSERT INTO portal_user (username, password, role, enabled) VALUES
    ('admin',   '$2a$10$OKKS0fGuD9I832rL8N6NLuSLjnVpKPYtJLBCVznSa9xBRTE6lkuAW', 'ROLE_ADMIN',   1);
INSERT INTO portal_user (username, password, role, enabled) VALUES
    ('kr_user', '$2a$10$OKKS0fGuD9I832rL8N6NLuSLjnVpKPYtJLBCVznSa9xBRTE6lkuAW', 'ROLE_KR_USER', 1);
INSERT INTO portal_user (username, password, role, enabled) VALUES
    ('us_user', '$2a$10$OKKS0fGuD9I832rL8N6NLuSLjnVpKPYtJLBCVznSa9xBRTE6lkuAW', 'ROLE_US_USER', 1);

-- ── 감사 로그 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
                                         id          INT PRIMARY KEY AUTO_INCREMENT,
                                         action      VARCHAR(50),
    entity      VARCHAR(50),
    entity_id   VARCHAR(100),
    before_data TEXT,
    after_data  TEXT,
    user_id     VARCHAR(50),
    ip_address  VARCHAR(50),
    created_at  DATETIME DEFAULT NOW()
    );
INSERT INTO audit_log (action,entity,entity_id,before_data,after_data,user_id,ip_address) VALUES ('CREATE','ORDER','ORD-KR-001',NULL,'status=PENDING','admin','127.0.0.1');
INSERT INTO audit_log (action,entity,entity_id,before_data,after_data,user_id,ip_address) VALUES ('UPDATE','ORDER','ORD-KR-001','status=PENDING','status=CONFIRMED','admin','127.0.0.1');
INSERT INTO audit_log (action,entity,entity_id,before_data,after_data,user_id,ip_address) VALUES ('CREATE','EMPLOYEE','EMP-001',NULL,'created by admin','admin','127.0.0.1');
INSERT INTO audit_log (action,entity,entity_id,before_data,after_data,user_id,ip_address) VALUES ('UPDATE','EMPLOYEE','EMP-003','base_salary=6000000','base_salary=6500000','admin','127.0.0.1');

-- ── 배치 로그 ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS batch_log (
                                         log_id             INT PRIMARY KEY AUTO_INCREMENT,
                                         batch_name         VARCHAR(50),
    country_code       VARCHAR(10),
    status             VARCHAR(20),
    started_at         DATETIME,
    ended_at           DATETIME,
    records_processed  INT DEFAULT 0,
    error_message      MEDIUMTEXT
    );
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('LARGE_PRODUCT_SYNC','KR','SUCCESS',NOW(),NOW(),100003);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('LARGE_PRODUCT_SYNC','US','SUCCESS',NOW(),NOW(),100003);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('EXCHANGE_RATE',NULL,'SUCCESS',NOW(),NOW(),5);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('SALES_AGGREGATE','KR','SUCCESS',NOW(),NOW(),800);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('SALES_AGGREGATE','US','SUCCESS',NOW(),NOW(),700);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('DATA_QUALITY','KR','SUCCESS',NOW(),NOW(),0);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('DATA_QUALITY','US','SUCCESS',NOW(),NOW(),0);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('ATTENDANCE_CLOSING',NULL,'SUCCESS',NOW(),NOW(),0);
INSERT INTO batch_log (batch_name,country_code,status,started_at,ended_at,records_processed) VALUES ('PAYROLL_CALCULATE',NULL,'SUCCESS',NOW(),NOW(),5);

-- ── 직원 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employee (
                                        emp_id      VARCHAR(20)   PRIMARY KEY,
    emp_name    VARCHAR(50)   NOT NULL,
    department  VARCHAR(50),
    position    VARCHAR(50),
    base_salary DECIMAL(12,2) NOT NULL,
    hire_date   DATE          NOT NULL,
    status      VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at  DATETIME      DEFAULT NOW(),
    updated_at  DATETIME      DEFAULT NOW()
    );
INSERT INTO employee VALUES ('EMP-001','김철수','영업팀','과장',5200000,'2021-03-15','ACTIVE',NOW(),NOW());
INSERT INTO employee VALUES ('EMP-002','이영희','마케팅팀','대리',3800000,'2022-06-01','ACTIVE',NOW(),NOW());
INSERT INTO employee VALUES ('EMP-003','박민준','개발팀','차장',6500000,'2020-01-10','ACTIVE',NOW(),NOW());
INSERT INTO employee VALUES ('EMP-004','최수진','인사팀','사원',3200000,'2024-02-01','ACTIVE',NOW(),NOW());
INSERT INTO employee VALUES ('EMP-005','정도현','영업팀','부장',7800000,'2018-05-20','ACTIVE',NOW(),NOW());

-- ── 근태 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance (
                                          id             INT PRIMARY KEY AUTO_INCREMENT,
                                          emp_id         VARCHAR(20),
    work_date      DATE,
    check_in       TIME,
    check_out      TIME,
    work_hours     DECIMAL(4,1) DEFAULT 0,
    overtime_hours DECIMAL(4,1) DEFAULT 0,
    status         VARCHAR(20)  DEFAULT 'NORMAL',
    created_at     DATETIME     DEFAULT NOW()
    );
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-001',CURDATE(),'09:00:00','18:30:00',8.5,0.5,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-002',CURDATE(),'09:20:00','18:00:00',7.7,0.0,'LATE');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-003',CURDATE(),'08:50:00','20:00:00',10.2,2.2,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-004',CURDATE(),NULL,NULL,0.0,0.0,'ABSENT');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-005',CURDATE(),'08:30:00','19:30:00',10.0,2.0,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-001',DATEADD(DAY,-1,CURDATE()),'09:00:00','18:00:00',8.0,0.0,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-002',DATEADD(DAY,-1,CURDATE()),'09:00:00','18:00:00',8.0,0.0,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-003',DATEADD(DAY,-1,CURDATE()),'08:30:00','20:30:00',11.0,3.0,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-004',DATEADD(DAY,-1,CURDATE()),'09:00:00','18:00:00',8.0,0.0,'NORMAL');
INSERT INTO attendance (emp_id,work_date,check_in,check_out,work_hours,overtime_hours,status) VALUES ('EMP-005',DATEADD(DAY,-1,CURDATE()),'08:00:00','19:00:00',10.0,2.0,'NORMAL');

-- ── 급여 ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payroll (
                                       id              INT PRIMARY KEY AUTO_INCREMENT,
                                       emp_id          VARCHAR(20),
    pay_year_month  VARCHAR(7),
    base_salary     DECIMAL(12,2) DEFAULT 0,
    overtime_pay    DECIMAL(12,2) DEFAULT 0,
    bonus           DECIMAL(12,2) DEFAULT 0,
    gross_pay       DECIMAL(12,2) DEFAULT 0,
    tax             DECIMAL(12,2) DEFAULT 0,
    insurance       DECIMAL(12,2) DEFAULT 0,
    total_deduction DECIMAL(12,2) DEFAULT 0,
    net_pay         DECIMAL(12,2) DEFAULT 0,
    status          VARCHAR(20)   DEFAULT 'CALCULATED',
    created_at      DATETIME      DEFAULT NOW()
    );
INSERT INTO payroll (emp_id,pay_year_month,base_salary,overtime_pay,bonus,gross_pay,tax,insurance,total_deduction,net_pay,status) VALUES ('EMP-001','2026-07',5200000,390000,0,5590000,184470,521088,705558,4884442,'CALCULATED');
INSERT INTO payroll (emp_id,pay_year_month,base_salary,overtime_pay,bonus,gross_pay,tax,insurance,total_deduction,net_pay,status) VALUES ('EMP-002','2026-07',3800000,0,0,3800000,125400,354160,479560,3320440,'CALCULATED');
INSERT INTO payroll (emp_id,pay_year_month,base_salary,overtime_pay,bonus,gross_pay,tax,insurance,total_deduction,net_pay,status) VALUES ('EMP-003','2026-07',6500000,877500,0,7377500,243458,687482,930940,6446560,'CALCULATED');
INSERT INTO payroll (emp_id,pay_year_month,base_salary,overtime_pay,bonus,gross_pay,tax,insurance,total_deduction,net_pay,status) VALUES ('EMP-004','2026-07',3200000,0,0,3200000,105600,298240,403840,2796160,'CALCULATED');
INSERT INTO payroll (emp_id,pay_year_month,base_salary,overtime_pay,bonus,gross_pay,tax,insurance,total_deduction,net_pay,status) VALUES ('EMP-005','2026-07',7800000,780000,0,8580000,283140,799576,1082716,7497284,'PAID');
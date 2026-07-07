
-- 기존 테이블 초기화 (재실행 대비)
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS employee;

-- ── 직원 테이블 ─────────────────────────────────────────
CREATE TABLE employee (
emp_id      VARCHAR(20)  PRIMARY KEY,
emp_name    VARCHAR(50)  NOT NULL,
department  VARCHAR(50)  NOT NULL,
position    VARCHAR(50),
base_salary DECIMAL(12,2) NOT NULL,
hire_date   DATE          NOT NULL,
status      VARCHAR(20)   DEFAULT 'ACTIVE',
created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ── 근태 테이블 ─────────────────────────────────────────
CREATE TABLE attendance (
id              BIGINT AUTO_INCREMENT PRIMARY KEY,
emp_id          VARCHAR(20) NOT NULL,
work_date       DATE        NOT NULL,
check_in        VARCHAR(10),
check_out       VARCHAR(10),
work_hours      DECIMAL(5,2) DEFAULT 0,
overtime_hours  DECIMAL(5,2) DEFAULT 0,
status          VARCHAR(20)  DEFAULT 'NORMAL',
FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
);

-- ── 급여 테이블 ─────────────────────────────────────────
CREATE TABLE payroll (
 id               BIGINT AUTO_INCREMENT PRIMARY KEY,
 emp_id           VARCHAR(20) NOT NULL,
 pay_year_month   VARCHAR(7)  NOT NULL,   -- 'yyyy-MM'
 base_salary      DECIMAL(12,2),
 overtime_pay     DECIMAL(12,2) DEFAULT 0,
 bonus            DECIMAL(12,2) DEFAULT 0,
 gross_pay        DECIMAL(12,2),
 tax              DECIMAL(12,2) DEFAULT 0,
 insurance        DECIMAL(12,2) DEFAULT 0,
 total_deduction  DECIMAL(12,2) DEFAULT 0,
 net_pay          DECIMAL(12,2),
 status           VARCHAR(20) DEFAULT 'CALCULATED',
 UNIQUE (emp_id, pay_year_month),
 FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
);

-- ══════════════════════════════════════════════════════
-- 데모 직원 데이터
-- ══════════════════════════════════════════════════════
INSERT INTO employee (emp_id, emp_name, department, position, base_salary, hire_date, status) VALUES
('E001', '김민준', '영업팀',   '과장', 4200000, '2021-03-15', 'ACTIVE'),
('E002', '이서연', '영업팀',   '대리', 3600000, '2022-07-01', 'ACTIVE'),
('E003', '박도윤', '개발팀',   '차장', 5100000, '2019-01-10', 'ACTIVE'),
('E004', '최지우', '개발팀',   '사원', 3300000, '2024-02-19', 'ACTIVE'),
('E005', '정하은', '인사팀',   '부장', 5800000, '2016-05-03', 'ACTIVE'),
('E006', '강도현', '물류팀',   '사원', 3200000, '2023-09-11', 'ACTIVE'),
('E007', '조유진', '재무팀',   '대리', 3700000, '2022-11-21', 'ACTIVE'),
('E008', '윤시우', '개발팀',   '사원', 3300000, '2024-06-01', 'LEAVE');

-- ══════════════════════════════════════════════════════
-- 데모 근태 데이터 (2026-07, 평일 기준 1~7일)
-- ══════════════════════════════════════════════════════
INSERT INTO attendance (emp_id, work_date, check_in, check_out, work_hours, overtime_hours, status) VALUES
-- E001
('E001', '2026-07-01', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E001', '2026-07-02', '09:05', '18:30', 8.0, 0.5, 'LATE'),
('E001', '2026-07-03', '09:00', '20:00', 8.0, 2.0, 'NORMAL'),
('E001', '2026-07-06', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E001', '2026-07-07', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),

-- E002
('E002', '2026-07-01', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E002', '2026-07-02', NULL, NULL, 0.0, 0.0, 'ABSENT'),
('E002', '2026-07-03', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E002', '2026-07-06', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E002', '2026-07-07', '09:10', '18:00', 7.8, 0.0, 'LATE'),

-- E003
('E003', '2026-07-01', '08:30', '19:00', 9.0, 1.0, 'NORMAL'),
('E003', '2026-07-02', '08:30', '19:00', 9.0, 1.0, 'NORMAL'),
('E003', '2026-07-03', '08:30', '18:30', 8.5, 0.5, 'NORMAL'),
('E003', '2026-07-06', '08:30', '19:30', 9.0, 1.5, 'NORMAL'),
('E003', '2026-07-07', '08:30', '18:00', 8.5, 0.0, 'NORMAL'),

-- E004
('E004', '2026-07-01', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E004', '2026-07-02', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E004', '2026-07-03', NULL, NULL, 0.0, 0.0, 'ABSENT'),
('E004', '2026-07-06', '09:00', '18:00', 8.0, 0.0, 'NORMAL'),
('E004', '2026-07-07', '09:00', '18:00', 8.0, 0.0, 'NORMAL');

-- ══════════════════════════════════════════════════════
-- 데모 급여 데이터 (2026-06, 지급완료 / 2026-07은 미계산 상태로 남겨둠)
-- ══════════════════════════════════════════════════════
INSERT INTO payroll (emp_id, pay_year_month, base_salary, overtime_pay, bonus,
    gross_pay, tax, insurance, total_deduction, net_pay, status) VALUES
    ('E001', '2026-06', 4200000, 150000, 0,       4350000, 310000, 190000, 500000, 3850000, 'PAID'),
    ('E002', '2026-06', 3600000, 0,      0,       3600000, 250000, 160000, 410000, 3190000, 'PAID'),
    ('E003', '2026-06', 5100000, 320000, 500000,  5920000, 450000, 260000, 710000, 5210000, 'PAID'),
    ('E004', '2026-06', 3300000, 80000,  0,       3380000, 230000, 150000, 380000, 3000000, 'PAID'),
    ('E005', '2026-06', 5800000, 0,      300000,  6100000, 480000, 280000, 760000, 5340000, 'PAID'),
    ('E006', '2026-06', 3200000, 120000, 0,       3320000, 220000, 145000, 365000, 2955000, 'PAID'),
('E007', '2026-06', 3700000, 0,      0,       3700000, 260000, 165000, 425000, 3275000, 'PAID');
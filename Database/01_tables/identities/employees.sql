
-- THAY ĐỔI: Xóa cột branch_id (workspace có nhiều chi nhánh,
--           event platform thì staff thuộc phòng ban, không thuộc chi nhánh).
--           Thêm cột department VARCHAR2(100).

CREATE TABLE Employees (
    employee_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       NUMBER(10) NOT NULL UNIQUE,
    employee_code VARCHAR2(50) UNIQUE NOT NULL,
    department    VARCHAR2(100),                   -- Thay branch_id: 'Kỹ thuật', 'Soát vé', 'Vận hành'

    position      VARCHAR2(100),                   -- Chức vụ
    hire_date     DATE,
    contract_type VARCHAR2(50),
    bank_number   VARCHAR2(50),
    job_status    VARCHAR2(50) DEFAULT 'ACTIVE',   -- ACTIVE, RESIGNED, ON_LEAVE
    identity_card VARCHAR2(20) UNIQUE,             -- CCCD/CMND

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted    NUMBER(1) DEFAULT 0 CHECK (is_deleted IN (0, 1))
);

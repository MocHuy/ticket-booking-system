CREATE TABLE Customers (
    customer_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            NUMBER(10) NOT NULL UNIQUE,
    membership_tier_id NUMBER,                     -- FK -> MembershipTiers

    customer_code      VARCHAR2(50) UNIQUE,
    loyalty_points     NUMBER DEFAULT 0,
    lifetime_spending  NUMBER(15, 2) DEFAULT 0,    -- Tổng chi tiêu tích lũy
    company_info       VARCHAR2(255),              -- Khách doanh nghiệp mua vé sự kiện

    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0 CHECK (is_deleted IN (0, 1))
);

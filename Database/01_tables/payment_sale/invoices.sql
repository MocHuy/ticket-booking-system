-- THAY ĐỔI: Thêm order_id để link trực tiếp Invoice -> Order
--   Lý do: Không cần JOIN qua InvoiceLines để tra "invoice của order X?"
--   1 Order = 1 Invoice (UNIQUE constraint)
CREATE TABLE Invoices (
    invoice_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        NUMBER NOT NULL UNIQUE,        -- FK -> Orders (1 order chỉ có 1 invoice)
    customer_id     NUMBER,                        -- FK -> Customers (NULL = khách vãng lai)
    staff_id        NUMBER,                        -- FK -> Employees (nhân viên xuất hóa đơn)

    sub_total       NUMBER(15, 2) DEFAULT 0,
    discount_amount NUMBER(15, 2) DEFAULT 0,
    total_amount    NUMBER(15, 2) DEFAULT 0,

    status          VARCHAR2(20) DEFAULT 'UNPAID', -- UNPAID, PAID, CANCELLED

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

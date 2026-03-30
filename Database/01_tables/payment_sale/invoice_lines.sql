
-- THAY ĐỔI: Cập nhật reference_type cho phù hợp nghiệp vụ mới.
--   Workspace: 'BOOKING','SESSION','F&B_ORDER','EXTENSION'
--   Event:     'ORDER','TICKET' (không có F&B_ORDER và EXTENSION)

CREATE TABLE InvoiceLines (
    line_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id     NUMBER NOT NULL,                -- FK -> Invoices
    reference_type VARCHAR2(50) NOT NULL,          -- 'ORDER' hoặc 'TICKET'
    reference_id   NUMBER NOT NULL,                -- order_id hoặc ticket_id tương ứng
    description    VARCHAR2(255) NOT NULL,
    unit_price     NUMBER(15, 2) NOT NULL,
    quantity       NUMBER NOT NULL,
    subtotal       NUMBER(15, 2) NOT NULL,         -- = unit_price * quantity

    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

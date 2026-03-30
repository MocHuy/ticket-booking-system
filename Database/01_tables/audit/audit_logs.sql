-- ================================================================
-- AuditLogs: Bảng ghi lại mọi hành động quan trọng trong hệ thống
-- Mục đích:
--   1. Truy vết hành vi người dùng (phân tích khách hàng - yêu cầu 2.5)
--   2. Phát hiện gian lận (bot mua vé, vé giả - risk scenario 2.8)
--   3. Audit trail cho các giao dịch tài chính
-- ================================================================
CREATE TABLE AuditLogs (
    log_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      NUMBER,                              -- FK -> Users (NULL = hành động hệ thống)
    action_type  VARCHAR2(50) NOT NULL,               -- LOGIN, TICKET_PURCHASE, TICKET_SCAN, PAYMENT_RETRY, SEAT_LOCK, SEAT_RELEASE
    entity_type  VARCHAR2(50),                        -- ORDER, TICKET, PAYMENT, EVENT, SEAT
    entity_id    NUMBER,                              -- ID của entity liên quan
    ip_address   VARCHAR2(50),                        -- IP client (chống bot)
    user_agent   VARCHAR2(500),                       -- Device/browser info
    details      CLOB,                                -- JSON chi tiết hành động
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

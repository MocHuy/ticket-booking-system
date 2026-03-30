-- ================================================================
-- Performance Indexes cho high-concurrency operations
-- Mục tiêu: Response time < 2 giây với 10,000 concurrent users
-- ================================================================

-- === TICKETS ===
-- Soát vé: tra cứu bằng ticket_code (QR scan) — O(1) lookup
-- Đây là index quan trọng nhất cho soát vé < 5 giây
CREATE INDEX idx_tickets_code ON Tickets(ticket_code);

-- Lấy danh sách vé theo sự kiện + trạng thái (cho offline sync)
CREATE INDEX idx_tickets_event_status ON Tickets(event_id, status);

-- Tra cứu vé theo customer
CREATE INDEX idx_tickets_customer ON Tickets(customer_id);

-- === SEATS ===
-- Sơ đồ ghế realtime: lấy tất cả ghế của 1 section theo trạng thái
CREATE INDEX idx_seats_section_status ON Seats(section_id, current_status);

-- Cleanup job: tìm ghế LOCKED đã hết hạn
CREATE INDEX idx_seats_locked_until ON Seats(current_status, locked_until)
    WHERE current_status = 'LOCKED';

-- === ORDERS ===
-- Cleanup job: tìm đơn hàng hết hạn chưa thanh toán
CREATE INDEX idx_orders_expired ON Orders(status, expired_at)
    WHERE status IN ('PENDING', 'AWAITING_PAYMENT');

-- Tra cứu đơn hàng theo sự kiện
CREATE INDEX idx_orders_event ON Orders(event_id, status);

-- Tra cứu đơn hàng theo customer
CREATE INDEX idx_orders_customer ON Orders(customer_id, status);

-- === TICKET SCANS ===
-- Check vé đã quét chưa (tránh quét lại)
CREATE INDEX idx_scans_ticket ON TicketScans(ticket_id, scan_result);

-- Báo cáo soát vé theo sự kiện
CREATE INDEX idx_scans_event ON TicketScans(event_id, scanned_at);

-- === TICKET TYPES ===
-- Danh sách loại vé active của 1 sự kiện
CREATE INDEX idx_tickettypes_event ON TicketTypes(event_id, is_active);

-- === EVENTS ===
-- Lọc sự kiện đang mở bán
CREATE INDEX idx_events_status ON Events(status, sale_start, sale_end);

-- === AUDIT LOGS ===
-- Tra cứu audit theo user
CREATE INDEX idx_audit_user ON AuditLogs(user_id, created_at);

-- Tra cứu audit theo entity
CREATE INDEX idx_audit_entity ON AuditLogs(entity_type, entity_id);

-- Tra cứu audit theo action
CREATE INDEX idx_audit_action ON AuditLogs(action_type, created_at);

-- === PAYMENTS ===
-- Tìm payment cần retry
CREATE INDEX idx_payments_retry ON Payments(status, retry_count)
    WHERE status = 'FAILED';

-- === VOUCHERS ===
-- Validate voucher code
CREATE INDEX idx_vouchers_code ON Vouchers(voucher_code, is_active);

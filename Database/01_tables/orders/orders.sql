--   Bookings = đặt chỗ không gian làm việc, có check-in/check-out.
--   Orders   = đơn mua vé sự kiện, không có khái niệm check-in/check-out.
--   Cấu trúc tài chính rất tương đồng:
--     total_amount, discount_amount, final_amount -> giữ nguyên logic
-- THAY ĐỔI:
--   - Xóa check_in_time, check_out_time, deposit_amount (không áp dụng)
--   - Xóa qr_code (QR gắn vào Ticket, không gắn vào Order)
--   - Thêm event_id: mỗi đơn hàng thuộc 1 sự kiện
--   - Thêm expired_at: timeout giỏ hàng (10 phút chưa thanh toán thì hủy)
--   - Thêm status AWAITING_PAYMENT thay vì PENDING riêng
--   - Thêm is_deleted: soft delete nhất quán với các bảng chính khác
CREATE TABLE Orders (
    order_id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id          NUMBER,                        -- FK -> Customers (NULL = khách vãng lai)
    event_id             NUMBER NOT NULL,               -- FK -> Events
    voucher_id           NUMBER,                        -- FK -> Vouchers

    order_code           VARCHAR2(50) UNIQUE NOT NULL,  -- VD: ORD-20260307-00001
    order_channel        VARCHAR2(20) DEFAULT 'ONLINE', -- ONLINE, OFFLINE (bán tại quầy)

    -- Tài chính (giữ nguyên logic từ Bookings)
    total_amount         NUMBER(15, 2) DEFAULT 0,       -- Tổng tiền trước giảm giá
    discount_amount      NUMBER(15, 2) DEFAULT 0,       -- Tiền được giảm
    final_amount         NUMBER(15, 2) DEFAULT 0,       -- Tiền thực thanh toán

    payment_status       VARCHAR2(20) DEFAULT 'UNPAID', -- UNPAID, PAID, REFUNDED

    status               VARCHAR2(30) DEFAULT 'PENDING',
    -- PENDING          : Vừa tạo, đang chọn ghế
    -- AWAITING_PAYMENT : Đã chọn ghế, chờ thanh toán
    -- PAID             : Đã thanh toán thành công
    -- CANCELLED        : Hủy (timeout hoặc khách hủy)
    -- REFUNDED         : Đã hoàn tiền

    expired_at           TIMESTAMP,                     -- Thời hạn thanh toán (10 phút từ lúc tạo)
    created_by_staff_id  NUMBER,                        -- Staff bán tại quầy (NULL = khách tự mua online)
    note                 VARCHAR2(255),

    -- Anti-bot tracking (risk scenario 2.8: tấn công bot khi mở bán vé)
    ip_address           VARCHAR2(50),                  -- IP client tạo đơn
    user_agent           VARCHAR2(500),                 -- Browser/device info

    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- [FIX MỚI] Thêm is_deleted: nhất quán soft delete với Users, Customers, Employees...
    -- Lý do: Đơn hàng bị CANCELLED / REFUNDED vẫn cần lưu lại để audit,
    -- báo cáo doanh thu, giải quyết tranh chấp. is_deleted = 1 chỉ dùng
    -- khi admin xóa thủ công các đơn rác (ví dụ: đơn test, đơn lỗi hệ thống).
    is_deleted           NUMBER(1) DEFAULT 0 CHECK (is_deleted IN (0, 1))
);

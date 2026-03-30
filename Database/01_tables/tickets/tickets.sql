-- NGUỒN GỐC: Hoàn toàn mới — không có bảng tương đương trong workspace
-- LÝ DO: Workspace dùng qr_code_token trong Spaces (quét để check-in chỗ ngồi).
--        Event ticketing cần bảng riêng cho từng vé điện tử, vì:
--        1. Mỗi vé có QR code độc lập (chống trùng lặp)
--        2. Vé cần lưu thông tin người cầm vé (holder) để kiểm soát gian lận
--        3. Vé có vòng đời riêng: VALID -> USED -> CANCELLED
--        4. Đây là bằng chứng thanh toán gửi cho khách qua email
--
-- QUAN HỆ SINH VÉ:
--   1 OrderDetail (quantity = N) -> sinh ra N Tickets
--   Mỗi Ticket = 1 QR code riêng biệt (UUID)
--   Ticket chỉ được tạo khi Orders.payment_status = 'PAID'
--
-- GHI CHÚ qr_code_data:
--   Không lưu ảnh QR — lưu chuỗi payload dạng signed JWT hoặc HMAC
--   để frontend/app gen ảnh QR động. Payload gồm: ticket_code + event_id
--   + issued_at + chữ ký bí mật (HMAC-SHA256). Điều này đảm bảo:
--   a) QR không thể làm giả nếu không có secret key của hệ thống.
--   b) App soát vé có thể verify offline bằng cách check chữ ký.
CREATE TABLE Tickets (
    ticket_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    detail_id       NUMBER NOT NULL,               -- FK -> OrderDetails

    -- Denormalize để tra cứu nhanh khi soát vé (không JOIN nhiều bảng)
    event_id        NUMBER NOT NULL,               -- FK -> Events
    customer_id     NUMBER,                        -- FK -> Customers
    ticket_type_id  NUMBER NOT NULL,               -- FK -> TicketTypes
    seat_id         NUMBER,                        -- FK -> Seats (NULL nếu Standing)

    -- Mã QR duy nhất (UUID) — trái tim chống vé giả
    ticket_code     VARCHAR2(100) UNIQUE NOT NULL, -- UUID sinh bởi hệ thống (SYS_GUID)
    qr_code_data    VARCHAR2(500) NOT NULL,        -- Signed payload để gen ảnh QR (HMAC/JWT)

    -- Thông tin người cầm vé (có thể khác customer mua)
    holder_name     VARCHAR2(100),
    holder_email    VARCHAR2(150),
    holder_phone    VARCHAR2(20),

    -- Trạng thái vé
    status          VARCHAR2(20) DEFAULT 'VALID',
    -- VALID    : Vé hợp lệ, chưa sử dụng
    -- USED     : Đã soát vé vào cổng
    -- CANCELLED: Bị hủy (do Order bị CANCELLED)
    -- REFUNDED : Đã hoàn tiền

    issued_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

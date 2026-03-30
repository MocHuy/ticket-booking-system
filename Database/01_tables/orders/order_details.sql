--   BookingDetails = chi tiết từng không gian trong đơn đặt chỗ.
--   OrderDetails   = chi tiết từng vé trong đơn mua vé.
-- GIỮ NGUYÊN:
--   - Quan hệ 1 Order -> nhiều OrderDetails
--   - price_at_booking -> price_snapshot: SNAPSHOT GIÁ tại thời điểm đặt
--     (đây là pattern quan trọng nhất, tránh sai lệch khi giá thay đổi)
-- THAY ĐỔI:
--   - space_id -> seat_id + ticket_type_id
--     (cần biết cả ghế ngồi lẫn loại vé để sinh Ticket)
--   - Xóa expected_start_time, expected_end_time (không áp dụng)
--   - Thêm quantity: 1 dòng có thể mua nhiều vé cùng loại
--     (đặc biệt với vé Standing không có ghế cụ thể)
CREATE TABLE OrderDetails (
    detail_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        NUMBER NOT NULL,               -- FK -> Orders
    ticket_type_id  NUMBER NOT NULL,               -- FK -> TicketTypes
    seat_id         NUMBER,                        -- FK -> Seats (NULL nếu Standing)

    quantity        NUMBER NOT NULL,               -- Số lượng vé loại này trong đơn
    price_snapshot  NUMBER(15, 2) NOT NULL,        -- Snapshot giá tại thời điểm đặt
    subtotal        NUMBER(15, 2) NOT NULL,        -- = quantity * price_snapshot

    note            VARCHAR2(255)
);

-- LÝ DO CHUYỂN ĐỔI:
--   Spaces = không gian vật lý cố định (bàn, phòng) trong chi nhánh.
--   Seats  = ghế vật lý trong 1 khu vực của 1 sự kiện.
--   Cả hai đều: có trạng thái AVAILABLE/BOOKED và thuộc về 1 đơn vị cha.
-- GIỮ NGUYÊN LOGIC: trạng thái AVAILABLE -> LOCKED -> SOLD
--                   (giống AVAILABLE -> BOOKED -> OCCUPIED trong workspace)
-- THAY ĐỔI:
--   - branch_id + type_id -> section_id (ghế thuộc khu vực, không chi nhánh)
--   - Xóa qr_code_token: QR gắn vào Ticket, không gắn vào ghế
--   - Thêm seat_label (A01, VIP-B03...), seat_row, seat_col
--   - Thêm locked_until: timeout giỏ hàng (10 phút, sau đó tự AVAILABLE lại)
--   - Đổi 'OCCUPIED' -> 'SOLD' (ghế đã bán, không phải đang ngồi)
--
-- [FIX MỚI] THÊM locked_by_order_id:
--   Lý do: Khi Seats.current_status = 'LOCKED', hệ thống cần biết
--   đơn hàng nào đang giữ ghế đó để:
--     1. Cleanup job: "Giải phóng ghế của các đơn hết hạn (expired_at < NOW())"
--        mà KHÔNG cần JOIN qua OrderDetails (JOIN = chậm khi 50.000 ghế).
--     2. Bảo mật: ngăn đơn hàng B giải phóng ghế đang bị đơn hàng A giữ.
--     3. Audit: biết ngay ghế X đang bị giữ bởi order_code nào.
CREATE TABLE Seats (
    seat_id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_id           NUMBER NOT NULL,                  -- FK -> Sections

    seat_label           VARCHAR2(20) NOT NULL,            -- VD: 'A01', 'VIP-B03'
    seat_row             VARCHAR2(10),
    seat_col             VARCHAR2(10),

    -- Trạng thái ghế
    current_status       VARCHAR2(20) DEFAULT 'AVAILABLE',
    -- AVAILABLE: Có thể mua
    -- LOCKED   : Đang trong giỏ hàng (chờ thanh toán, timeout 10 phút)
    -- SOLD     : Đã bán thành công
    -- DISABLED : Ghế bị vô hiệu (hỏng, chặn tầm nhìn...)

    locked_until         TIMESTAMP,                        -- Thời điểm hết hạn LOCKED

    -- [FIX MỚI] Đơn hàng đang giữ ghế này (NULL nếu không bị lock)
    locked_by_order_id   NUMBER,                           -- FK -> Orders (ON DELETE SET NULL)

    is_active            NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

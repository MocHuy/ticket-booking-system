-- LÝ DO: Workspace dùng Spaces (1 tầng: bàn/phòng).
--        Event ticketing cần 2 tầng: Sections (khu vực) -> Seats (ghế).
--        Ví dụ: Sự kiện có khu VIP, khu A, khu B, khu Standing.
--        Sections định nghĩa layout màu sắc trên sơ đồ ghế.
--
-- [FIX] THÊM section_type:
--   Lý do: Bản gốc không phân biệt khu ngồi (có ghế đánh số) và khu
--   đứng (standing, không có ghế cụ thể). Điều này gây ra 2 vấn đề:
--     1. Trigger tạo Seats không biết khi nào cần sinh ghế, khi nào không.
--     2. OrderDetails.quantity có thể > 1 cho Standing nhưng = 1 cho Seated.
--        Không có section_type thì không thể validate ở tầng DB.
--   Giải pháp: Thêm cột section_type VARCHAR2(20) DEFAULT 'SEATED'.

CREATE TABLE Sections (
    section_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id     NUMBER NOT NULL,                  -- FK -> Events
    section_name VARCHAR2(100) NOT NULL,           -- 'Khu VIP', 'Khu A', 'Standing'
    total_seats  NUMBER NOT NULL,
    color_code   VARCHAR2(10) DEFAULT '#CCCCCC',   -- Màu hiển thị trên sơ đồ ghế

    -- [FIX MỚI] Phân loại khu vực
    -- SEATED  : Có ghế đánh số cụ thể (gen Seats riêng cho từng ghế)
    -- STANDING: Khu đứng/khu vực không đánh số ghế
    --           OrderDetails.seat_id = NULL, quantity có thể > 1
    section_type VARCHAR2(20) DEFAULT 'SEATED',

    -- Toạ độ khu vực trên canvas sơ đồ (dùng cho frontend vẽ sơ đồ ghế)
    position_x   NUMBER,
    position_y   NUMBER,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

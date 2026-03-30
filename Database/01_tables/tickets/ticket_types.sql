-- LÝ DO CHUYỂN ĐỔI:
--   SpaceTypes = loại không gian (phòng họp, bàn cá nhân) với giá theo giờ.
--   TicketTypes = loại vé (VIP, GA, Early Bird) với giá cố định.
--   Cả hai đều: định nghĩa "loại sản phẩm" với price, description, img_url.
-- GIỮ NGUYÊN: name, description, img_url, is_active, timestamps
-- THAY ĐỔI:
--   - base_price_per_hour/day -> price (giá vé cố định, không theo giờ)
--   - capacity (capacity phòng) -> max_per_order (giới hạn vé/đơn hàng)
--   - Thêm event_id: TicketType gắn với 1 sự kiện cụ thể
--   - Thêm section_id: TicketType gắn với khu vực (NULL = áp dụng toàn sự kiện)
--   - Thêm quantity_total / quantity_sold: quản lý số lượng vé
--   - Thêm sale_start / sale_end: Early Bird có thể mở bán sớm hơn

CREATE TABLE TicketTypes (
    ticket_type_id  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id        NUMBER NOT NULL,                -- FK -> Events
    section_id      NUMBER,                         -- FK -> Sections (NULL = toàn sự kiện)

    type_name       VARCHAR2(100) NOT NULL,          -- 'VIP', 'GA', 'Early Bird', 'Student'
    price           NUMBER(15, 2) NOT NULL,
    description     VARCHAR2(500),
    img_url         VARCHAR2(500),

    -- Quản lý số lượng vé
    quantity_total  NUMBER NOT NULL,               -- Tổng số vé phát hành
    quantity_sold   NUMBER DEFAULT 0,              -- Đã bán (thanh toán thành công)
    quantity_locked NUMBER DEFAULT 0,              -- Đang trong giỏ hàng chờ thanh toán

    -- Thời gian mở bán riêng (override từ Events.sale_start nếu có)
    sale_start      TIMESTAMP,
    sale_end        TIMESTAMP,

    max_per_order   NUMBER DEFAULT 4,               -- Tối đa bao nhiêu vé loại này/đơn hàng

    is_active       NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

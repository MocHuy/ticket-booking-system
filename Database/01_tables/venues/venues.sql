-- THAY ĐỔI:
--   - Đổi tên bảng Branches -> Venues
--   - Đổi branch_id -> venue_id
--   - Xóa manager_id (venue không cần manager riêng trong hệ thống)
--   - Thêm capacity: sức chứa tối đa, ràng buộc tổng ghế
--   - Thêm venue_type: loại địa điểm (INDOOR/OUTDOOR/STADIUM)
-- ================================================================
CREATE TABLE Venues (
    venue_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_name       VARCHAR2(200) NOT NULL,
    address          VARCHAR2(255) NOT NULL,
    city             VARCHAR2(100),
    capacity         NUMBER NOT NULL,              -- Sức chứa tối đa (ràng buộc tổng Seats)
    venue_type       VARCHAR2(20) DEFAULT 'INDOOR', -- INDOOR, OUTDOOR, STADIUM
    location_map_url VARCHAR2(500),
    hotline          VARCHAR2(20),
    is_active        NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),

    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

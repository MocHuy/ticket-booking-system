-- [FIX 1] THÊM event_id (nullable):
--   Lý do: Workspace dùng voucher chung cho mọi đặt chỗ.
--   Event ticketing thường có 2 loại voucher:
--     - Voucher toàn hệ thống (event_id = NULL): dùng được cho mọi sự kiện
--     - Voucher riêng sự kiện (event_id = X): chỉ áp dụng cho concert X
--   Không có cột này, không thể kiểm soát việc dùng "voucher concert A"
--   để mua vé "concert B". Trigger check_voucher sẽ cần cột này.
--   Giữ NULL = cho phép dùng toàn hệ thống, không bắt buộc.
--
-- [FIX 2] THÊM valid_from:
--   Lý do: Bản gốc chỉ có expiry_date (ngày hết hạn), nhưng thiếu
--   ngày bắt đầu hiệu lực. Dẫn đến không thể tạo voucher "dùng được
--   từ ngày 15/3 đến 30/3" — voucher sẽ active ngay khi tạo.
--   Với event ticketing (early bird, flash sale), valid_from rất quan trọng.
CREATE TABLE Vouchers (
    voucher_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_code    VARCHAR2(50) UNIQUE NOT NULL,
    discount_type   VARCHAR2(20) NOT NULL,          -- PERCENT hoặc FIXED
    discount_value  NUMBER(15, 2) NOT NULL,
    min_order_value NUMBER(15, 2) DEFAULT 0,
    max_discount    NUMBER(15, 2),                  -- Trần giảm tối đa (cho loại PERCENT)
    usage_limit     NUMBER,
    used_count      NUMBER DEFAULT 0,

    -- [FIX MỚI 1] Scope voucher về 1 sự kiện cụ thể (NULL = toàn hệ thống)
    event_id        NUMBER,                         -- FK -> Events (nullable)

    -- [FIX MỚI 2] Ngày bắt đầu hiệu lực
    valid_from      DATE,                           -- NULL = có hiệu lực ngay khi tạo

    expiry_date     DATE NOT NULL,
    is_active       NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

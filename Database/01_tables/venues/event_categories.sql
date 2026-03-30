-- LÝ DO CHUYỂN ĐỔI:
--   Categories trong workspace phân loại đồ ăn/thức uống.
--   EventCategories phân loại sự kiện (Concert, Workshop, Hội thảo...).
--   Cấu trúc giống hệt nhau: name, description, is_active, timestamps.
--   Chỉ đổi tên bảng và tên cột cho phù hợp ngữ cảnh.
CREATE TABLE EventCategories (
    category_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_name VARCHAR2(100) NOT NULL UNIQUE,   -- Concert, Hội thảo, Workshop, Festival
    description   VARCHAR2(255),
    is_active     NUMBER(1) DEFAULT 1 CHECK (is_active IN (0, 1)),

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

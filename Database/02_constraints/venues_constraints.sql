    -- CHECK Constraints: Venues
ALTER TABLE Venues
ADD CONSTRAINT chk_venue_type CHECK (venue_type IN ('INDOOR', 'OUTDOOR', 'STADIUM'));

ALTER TABLE Venues
ADD CONSTRAINT chk_venue_capacity CHECK (capacity > 0);

-- CHECK Constraints: Events
ALTER TABLE Events
ADD CONSTRAINT chk_event_status
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ON_SALE', 'SOLD_OUT', 'CANCELLED', 'COMPLETED'));

ALTER TABLE Events
ADD CONSTRAINT chk_event_time CHECK (end_datetime > start_datetime);

ALTER TABLE Events
ADD CONSTRAINT chk_event_sale_time CHECK (sale_end > sale_start);

-- [FIX MỚI] sale_end không được sau thời điểm sự kiện bắt đầu
-- Lý do: Không hợp lý khi vẫn bán vé sau khi concert đã bắt đầu.
-- Cho phép bằng nhau (bán đến phút cuối trước khi event diễn ra).
ALTER TABLE Events
ADD CONSTRAINT chk_event_sale_before_start CHECK (sale_end <= start_datetime);

ALTER TABLE Events
ADD CONSTRAINT chk_event_capacity CHECK (total_capacity > 0);

-- CHECK Constraints: Sections
ALTER TABLE Sections
ADD CONSTRAINT chk_section_seats CHECK (total_seats > 0);

-- [FIX MỚI] Validate section_type
ALTER TABLE Sections
ADD CONSTRAINT chk_section_type CHECK (section_type IN ('SEATED', 'STANDING'));

-- Foreign Keys: Events
ALTER TABLE Events
ADD CONSTRAINT fk_events_category
FOREIGN KEY (category_id) REFERENCES EventCategories(category_id);

ALTER TABLE Events
ADD CONSTRAINT fk_events_venue
FOREIGN KEY (venue_id) REFERENCES Venues(venue_id);

ALTER TABLE Events
ADD CONSTRAINT fk_events_organizer
FOREIGN KEY (organizer_id) REFERENCES Employees(employee_id);

-- Foreign Keys: Sections
ALTER TABLE Sections
ADD CONSTRAINT fk_sections_events
FOREIGN KEY (event_id) REFERENCES Events(event_id) ON DELETE CASCADE;

-- [FIX MỚI] Không cho phép 2 khu vực trùng tên trong cùng 1 sự kiện
-- Lý do: Nếu sự kiện có 2 Sections tên "Khu A", frontend sơ đồ ghế
-- và báo cáo doanh thu sẽ không phân biệt được, gây nhầm lẫn nghiêm trọng.
ALTER TABLE Sections
ADD CONSTRAINT uq_section_name_per_event UNIQUE (event_id, section_name);

-- ----------------------------------------------------------------
-- CHECK + FK Constraints: Seats
-- (Seats nằm trong 01_tables/venues/ nên constraints đặt ở đây)
-- ----------------------------------------------------------------
ALTER TABLE Seats
ADD CONSTRAINT chk_seat_status
CHECK (current_status IN ('AVAILABLE', 'LOCKED', 'SOLD', 'DISABLED'));

-- locked_until chỉ có nghĩa khi current_status = 'LOCKED'
-- Nếu status != 'LOCKED' thì locked_until phải NULL
-- Lý do: Tránh dữ liệu thừa — ghế SOLD mà vẫn có locked_until = gây nhầm lẫn audit
ALTER TABLE Seats
ADD CONSTRAINT chk_seat_locked_until
CHECK (current_status = 'LOCKED' OR locked_until IS NULL);

-- locked_by_order_id chỉ có nghĩa khi LOCKED (đối xứng với locked_until)
ALTER TABLE Seats
ADD CONSTRAINT chk_seat_locked_by_order
CHECK (current_status = 'LOCKED' OR locked_by_order_id IS NULL);

-- Foreign Key: Seats -> Sections
ALTER TABLE Seats
ADD CONSTRAINT fk_seats_sections
FOREIGN KEY (section_id) REFERENCES Sections(section_id) ON DELETE CASCADE;

-- UNIQUE: không được có 2 ghế cùng nhãn trong 1 section
ALTER TABLE Seats
ADD CONSTRAINT uq_seat_label UNIQUE (section_id, seat_label);

-- GHI CHÚ: FK Seats.locked_by_order_id -> Orders
-- Khai báo trong 02_constraints/orders_constraints.sql
-- (vì FK tham chiếu bảng Orders thuộc module orders, deploy sau venues)

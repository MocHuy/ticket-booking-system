-- ================================================================
-- Seed Data — Dữ liệu mẫu cho hệ thống Dề Dê Ticket Booking
-- Chạy SAU khi đã tạo tables + constraints + indexes
-- ================================================================

-- === MEMBERSHIP TIERS ===
INSERT INTO MembershipTiers (tier_name, min_points, discount_percent, description) VALUES ('BRONZE', 0, 0, 'Hạng đồng - Mặc định');
INSERT INTO MembershipTiers (tier_name, min_points, discount_percent, description) VALUES ('SILVER', 1000, 5, 'Hạng bạc - Giảm 5%');
INSERT INTO MembershipTiers (tier_name, min_points, discount_percent, description) VALUES ('GOLD', 5000, 10, 'Hạng vàng - Giảm 10%');
INSERT INTO MembershipTiers (tier_name, min_points, discount_percent, description) VALUES ('DIAMOND', 20000, 15, 'Hạng kim cương - Giảm 15%');

-- === FUNCTIONS (Chức năng hệ thống) ===
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_EVENT_MANAGE', 'Quản lý sự kiện', 'EVENT');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_TICKET_MANAGE', 'Quản lý vé', 'TICKET');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_TICKET_SCAN', 'Soát vé', 'TICKET');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_PAYMENT_MANAGE', 'Quản lý thanh toán', 'PAYMENT');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_REPORT_VIEW', 'Xem báo cáo', 'REPORT');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_USER_MANAGE', 'Quản lý người dùng', 'USER');
INSERT INTO Functions (function_code, function_name, module_name) VALUES ('FUNC_VENUE_MANAGE', 'Quản lý địa điểm', 'VENUE');

-- === ROLES ===
INSERT INTO Roles (role_name, description, is_system_role) VALUES ('ADMIN', 'Quản trị viên hệ thống', 1);
INSERT INTO Roles (role_name, description, is_system_role) VALUES ('ORGANIZER', 'Người tổ chức sự kiện', 1);
INSERT INTO Roles (role_name, description, is_system_role) VALUES ('SCANNER', 'Nhân viên soát vé', 1);
INSERT INTO Roles (role_name, description, is_system_role) VALUES ('CUSTOMER', 'Khách hàng', 1);

-- === EVENT CATEGORIES ===
INSERT INTO EventCategories (category_name, description) VALUES ('Concert', 'Buổi biểu diễn âm nhạc');
INSERT INTO EventCategories (category_name, description) VALUES ('Workshop', 'Hội thảo thực hành');
INSERT INTO EventCategories (category_name, description) VALUES ('Conference', 'Hội nghị chuyên đề');
INSERT INTO EventCategories (category_name, description) VALUES ('Festival', 'Lễ hội');
INSERT INTO EventCategories (category_name, description) VALUES ('Exhibition', 'Triển lãm');

-- === VENUES ===
INSERT INTO Venues (venue_name, address, city, capacity, venue_type, hotline)
VALUES ('Nhà hát Hòa Bình', '240 Đường 3 Tháng 2, Quận 10', 'TP.HCM', 2500, 'INDOOR', '028-3865-5050');

INSERT INTO Venues (venue_name, address, city, capacity, venue_type, hotline)
VALUES ('Sân vận động Phú Thọ', '1 Lữ Gia, Quận 11', 'TP.HCM', 15000, 'STADIUM', '028-3855-7777');

INSERT INTO Venues (venue_name, address, city, capacity, venue_type, hotline)
VALUES ('Trung tâm Hội nghị GEM Center', '8 Nguyễn Bỉnh Khiêm, Quận 1', 'TP.HCM', 3000, 'INDOOR', '028-3825-6325');

INSERT INTO Venues (venue_name, address, city, capacity, venue_type, hotline)
VALUES ('Công viên Yên Sở', 'Đường Ngọc Hồi, Hoàng Mai', 'Hà Nội', 50000, 'OUTDOOR', '024-3861-1111');

-- === USERS (Admin + Sample Staff) ===
-- Password hash = BCrypt("password123")
INSERT INTO Users (username, password_hash, email, status, full_name, phone_number, gender)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@dede.vn', 'ACTIVE', 'Admin Hệ Thống', '0901000001', 'MALE');

INSERT INTO Users (username, password_hash, email, status, full_name, phone_number, gender)
VALUES ('organizer01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'organizer@dede.vn', 'ACTIVE', 'Nguyễn Văn Tổ Chức', '0901000002', 'MALE');

INSERT INTO Users (username, password_hash, email, status, full_name, phone_number, gender)
VALUES ('scanner01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'scanner@dede.vn', 'ACTIVE', 'Trần Thị Soát Vé', '0901000003', 'FEMALE');

INSERT INTO Users (username, password_hash, email, status, full_name, phone_number, gender)
VALUES ('customer01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'customer01@gmail.com', 'ACTIVE', 'Lê Văn Khách', '0901000004', 'MALE');

COMMIT;

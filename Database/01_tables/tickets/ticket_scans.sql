-- LÝ DO CHUYỂN ĐỔI:
--   Sessions = phiên làm việc có thời gian dài (check-in -> check-out, tính phí).
--   TicketScans = sự kiện soát vé đơn lẻ (quét QR -> vào cổng, 1 lần duy nhất).
-- GIỮ NGUYÊN:
--   - check_in_staff_id -> scanned_by: nhân viên thực hiện
--   - Ghi nhận thời điểm thực hiện (checkin_time -> scanned_at)
-- THAY ĐỔI:
--   - Xóa checkout_time, applied_hourly_rate, check_out_staff_id (không áp dụng)
--   - Xóa space_id, booking_id -> thay bằng ticket_id
--   - Thêm scan_result: SUCCESS/ALREADY_USED/INVALID/CANCELLED
--   - Thêm is_offline_scan + synced_at: hỗ trợ offline (yêu cầu đặc biệt 2.4)
--     App soát vé lưu local khi mất mạng, sync về DB khi có mạng lại
--   - Thêm device_id: nhận diện thiết bị quét để audit
--   - Thêm scan_location: cổng nào (Cổng A, Cổng B...)
--
-- [GIỮ NGUYÊN] event_id DENORMALIZE:
--   Khi offline, app cần tải trước danh sách vé hợp lệ của 1 sự kiện
--   để validate tại chỗ mà không cần Internet.
--   Nếu không có event_id, app phải JOIN: TicketScans -> Tickets -> Events
--   Trên SQLite local của thiết bị offline, JOIN nhiều bảng = chậm + lỗi.
--   event_id denormalize giúp:
--     1. Query "tất cả scan của event X hôm nay" = 1 bảng, 1 cột index.
--     2. Audit đơn giản: biết ngay scan thuộc sự kiện nào.
--     3. Sync log rõ ràng: server biết batch này thuộc event nào.
-- ================================================================
CREATE TABLE TicketScans (
    scan_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id       NUMBER NOT NULL,               -- FK -> Tickets
    scanned_by      NUMBER NOT NULL,               -- FK -> Employees (nhân viên soát vé)

    -- Denormalize event_id để hỗ trợ offline query
    event_id        NUMBER NOT NULL,               -- FK -> Events (lấy từ Tickets.event_id khi insert)

    scan_result     VARCHAR2(20) NOT NULL,
    -- SUCCESS      : Vé hợp lệ, cho vào
    -- ALREADY_USED : Vé đã được quét trước đó (phát hiện vé giả/dùng lại)
    -- INVALID      : Mã QR không tồn tại trong hệ thống
    -- CANCELLED    : Vé đã bị hủy

    scan_location   VARCHAR2(100),                 -- 'Cổng A', 'Cổng B', 'Cổng VIP'
    device_id       VARCHAR2(100),                 -- ID thiết bị quét (để audit)

    -- Hỗ trợ offline (yêu cầu đặc biệt đề tài 2.4)
    scanned_at      TIMESTAMP NOT NULL,            -- Thời điểm quét thực tế trên thiết bị
    synced_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Thời điểm đồng bộ lên server
    is_offline_scan NUMBER(1) DEFAULT 0 CHECK (is_offline_scan IN (0, 1))
    -- = 1 nếu quét khi mất mạng, sau đó sync lên
);

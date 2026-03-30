-- THAY ĐỔI:
--   - Thêm retry_count + last_retry_at:
--     ĐÂY LÀ YÊU CẦU ĐẶC BIỆT CỦA ĐỀ TÀI (mục 2.4):
--     "Hệ thống phải có cơ chế retry khi thanh toán thất bại"
--     retry_count đếm số lần thử lại, last_retry_at lưu lần thử cuối.
--   - Thêm max_retry_count: giới hạn số lần retry tối đa (mặc định 3).
--     Lý do: Nếu không giới hạn, vòng lặp retry có thể chạy vô hạn
--     khi payment gateway lỗi kéo dài -> tốn tài nguyên + DDoS nội bộ.
--     Khi retry_count >= max_retry_count, hệ thống tự CANCEL Order
--     và giải phóng ghế (xử lý bởi trigger hoặc scheduled job).
--   - Mở rộng payment_method: thêm SIMULATED (payment gateway giả lập)
CREATE TABLE Payments (
    payment_id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id            NUMBER NOT NULL,

    payment_method        VARCHAR2(50) NOT NULL,    -- CASH, CREDIT_CARD, VNPAY, MOMO, SIMULATED
    amount                NUMBER(15, 2) NOT NULL,
    status                VARCHAR2(20) DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
    transaction_reference VARCHAR2(255),            -- Mã giao dịch từ payment gateway
    payment_date          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Cơ chế retry (yêu cầu đặc biệt đề tài 2.4)
    retry_count           NUMBER DEFAULT 0,         -- Số lần đã thử lại
    last_retry_at         TIMESTAMP,                -- Thời điểm thử lại gần nhất
    max_retry_count       NUMBER DEFAULT 3,         -- Giới hạn retry tối đa

    note                  VARCHAR2(255)
);

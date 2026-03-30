-- CHECK Constraints: Vouchers
ALTER TABLE Vouchers
ADD CONSTRAINT chk_voucher_type CHECK (discount_type IN ('PERCENT', 'FIXED'));

ALTER TABLE Vouchers
ADD CONSTRAINT chk_voucher_value CHECK (discount_value > 0);

ALTER TABLE Vouchers
ADD CONSTRAINT chk_voucher_used CHECK (used_count >= 0);

-- [FIX MỚI] valid_from phải trước expiry_date nếu được cung cấp
-- Lý do: Nếu valid_from = 2026-03-20 nhưng expiry_date = 2026-03-15,
-- voucher không bao giờ hợp lệ — đây là data lỗi cần chặn sớm.
ALTER TABLE Vouchers
ADD CONSTRAINT chk_voucher_validity
CHECK (valid_from IS NULL OR valid_from < expiry_date);

-- [FIX MỚI] FK Vouchers -> Events (scope voucher về sự kiện cụ thể)
-- NULL = voucher áp dụng cho toàn hệ thống (không giới hạn sự kiện)
ALTER TABLE Vouchers
ADD CONSTRAINT fk_vouchers_events
FOREIGN KEY (event_id) REFERENCES Events(event_id);

-- CHECK Constraints: Invoices
ALTER TABLE Invoices
ADD CONSTRAINT chk_invoice_status CHECK (status IN ('UNPAID', 'PAID', 'CANCELLED'));

ALTER TABLE Invoices
ADD CONSTRAINT chk_invoice_amounts
CHECK (sub_total >= 0 AND discount_amount >= 0 AND total_amount >= 0);

-- discount không được vượt sub_total
ALTER TABLE Invoices
ADD CONSTRAINT chk_invoice_discount CHECK (discount_amount <= sub_total);

-- CHECK Constraints: InvoiceLines
-- THAY ĐỔI so với workspace: đổi từ 'BOOKING','SESSION' sang 'ORDER','TICKET'
ALTER TABLE InvoiceLines
ADD CONSTRAINT chk_reference_type CHECK (reference_type IN ('ORDER', 'TICKET'));

ALTER TABLE InvoiceLines
ADD CONSTRAINT chk_invoiceline_qty CHECK (quantity > 0);

ALTER TABLE InvoiceLines
ADD CONSTRAINT chk_invoiceline_price CHECK (unit_price >= 0);

-- CHECK Constraints: Payments
-- Thêm SIMULATED cho payment gateway giả lập (yêu cầu đề tài)
ALTER TABLE Payments
ADD CONSTRAINT chk_payment_method
CHECK (payment_method IN ('CASH', 'CREDIT_CARD', 'VNPAY', 'MOMO', 'SIMULATED'));

ALTER TABLE Payments
ADD CONSTRAINT chk_payment_status
CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'));

ALTER TABLE Payments
ADD CONSTRAINT chk_payment_amount CHECK (amount > 0);

ALTER TABLE Payments
ADD CONSTRAINT chk_payment_retry CHECK (retry_count >= 0);

-- Foreign Keys: Payments
ALTER TABLE Payments
ADD CONSTRAINT fk_payments_invoices
FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id);

-- Foreign Keys: Invoices
ALTER TABLE Invoices
ADD CONSTRAINT fk_invoices_orders
FOREIGN KEY (order_id) REFERENCES Orders(order_id);

ALTER TABLE Invoices
ADD CONSTRAINT fk_invoices_customers
FOREIGN KEY (customer_id) REFERENCES Customers(customer_id);

ALTER TABLE Invoices
ADD CONSTRAINT fk_invoices_staff
FOREIGN KEY (staff_id) REFERENCES Employees(employee_id);

-- Foreign Keys: InvoiceLines
ALTER TABLE InvoiceLines
ADD CONSTRAINT fk_invoicelines_invoices
FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id) ON DELETE CASCADE;

-- ----------------------------------------------------------------
-- [FIX MỚI] CHECK Constraints bổ sung: Payments
-- ----------------------------------------------------------------

-- Giới hạn số lần retry tối đa (không được vượt max_retry_count)
-- Lý do: retry_count > max_retry_count = bug logic ứng dụng; constraint này
-- là lưới an toàn cuối tránh vòng lặp retry vô hạn làm treo hệ thống.
ALTER TABLE Payments
ADD CONSTRAINT chk_payment_retry_limit
CHECK (retry_count <= max_retry_count);

ALTER TABLE Payments
ADD CONSTRAINT chk_payment_max_retry CHECK (max_retry_count > 0);

-- ----------------------------------------------------------------
-- [FIX MỚI] CHECK Constraints bổ sung: Vouchers
-- ----------------------------------------------------------------

-- used_count không được vượt usage_limit (nếu có giới hạn)
-- Lý do: Không có constraint này, nếu trigger cập nhật used_count bị lỗi
-- thì DB có thể có used_count > usage_limit mà không bị chặn.
ALTER TABLE Vouchers
ADD CONSTRAINT chk_voucher_used_limit
CHECK (usage_limit IS NULL OR used_count <= usage_limit);

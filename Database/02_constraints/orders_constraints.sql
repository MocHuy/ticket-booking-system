-- CHECK Constraints: Orders
ALTER TABLE Orders
ADD CONSTRAINT chk_order_status
CHECK (status IN ('PENDING', 'AWAITING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDED'));

ALTER TABLE Orders
ADD CONSTRAINT chk_order_payment_status
CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED'));

ALTER TABLE Orders
ADD CONSTRAINT chk_order_channel
CHECK (order_channel IN ('ONLINE', 'OFFLINE'));

ALTER TABLE Orders
ADD CONSTRAINT chk_order_amounts
CHECK (total_amount >= 0 AND discount_amount >= 0 AND final_amount >= 0);

-- discount không được vượt tổng tiền
ALTER TABLE Orders
ADD CONSTRAINT chk_order_discount CHECK (discount_amount <= total_amount);

ALTER TABLE Orders
ADD CONSTRAINT chk_order_final CHECK (final_amount <= total_amount);

-- [FIX MỚI] expired_at phải sau thời điểm tạo đơn (nếu có)
-- Lý do: expired_at = created_at hoặc nhỏ hơn -> giỏ hàng hết hạn ngay lập tức,
-- khách không kịp thanh toán. Bug này gây ra UX tệ và khó debug.
ALTER TABLE Orders
ADD CONSTRAINT chk_order_expired_at
CHECK (expired_at IS NULL OR expired_at > created_at);


-- CHECK Constraints: OrderDetails
ALTER TABLE OrderDetails
ADD CONSTRAINT chk_orderdetail_qty CHECK (quantity > 0);

ALTER TABLE OrderDetails
ADD CONSTRAINT chk_orderdetail_price CHECK (price_snapshot >= 0);

ALTER TABLE OrderDetails
ADD CONSTRAINT chk_orderdetail_subtotal
CHECK (subtotal = quantity * price_snapshot);
-- Lý do: subtotal là computed value; constraint này đảm bảo data không bị
-- insert sai từ bug ứng dụng (tính sai subtotal khi có discount riêng dòng).

-- [GIỮ NGUYÊN] Khi đã chọn ghế cụ thể (seat_id NOT NULL), quantity phải = 1
-- Lý do: 1 dòng OrderDetails không thể đại diện cho "2 ghế A01 cùng lúc".
-- Ghế A01 là vật lý duy nhất. Chỉ vé Standing (seat_id NULL) mới được quantity > 1.
ALTER TABLE OrderDetails
ADD CONSTRAINT chk_orderdetail_seat_qty
CHECK (seat_id IS NULL OR quantity = 1);


-- Foreign Keys: Orders
ALTER TABLE Orders
ADD CONSTRAINT fk_orders_customers
FOREIGN KEY (customer_id) REFERENCES Customers(customer_id);

ALTER TABLE Orders
ADD CONSTRAINT fk_orders_events
FOREIGN KEY (event_id) REFERENCES Events(event_id);

ALTER TABLE Orders
ADD CONSTRAINT fk_orders_vouchers
FOREIGN KEY (voucher_id) REFERENCES Vouchers(voucher_id);

ALTER TABLE Orders
ADD CONSTRAINT fk_orders_staff
FOREIGN KEY (created_by_staff_id) REFERENCES Employees(employee_id);


-- Foreign Keys: OrderDetails
ALTER TABLE OrderDetails
ADD CONSTRAINT fk_orderdetails_orders
FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE;

ALTER TABLE OrderDetails
ADD CONSTRAINT fk_orderdetails_tickettypes
FOREIGN KEY (ticket_type_id) REFERENCES TicketTypes(ticket_type_id);

ALTER TABLE OrderDetails
ADD CONSTRAINT fk_orderdetails_seats
FOREIGN KEY (seat_id) REFERENCES Seats(seat_id);

-- ----------------------------------------------------------------
-- Foreign Keys: Seats.locked_by_order_id (khai báo tại đây)
-- Lý do: FK ngược từ Seats -> Orders; phải add sau khi Orders đã tồn tại.
-- ON DELETE SET NULL: khi Order bị hard-delete, ghế tự NULL locked_by_order_id
-- (trigger sẽ xử lý đặt lại current_status = 'AVAILABLE').
-- ----------------------------------------------------------------
ALTER TABLE Seats
ADD CONSTRAINT fk_seats_locked_by_order
FOREIGN KEY (locked_by_order_id) REFERENCES Orders(order_id) ON DELETE SET NULL;

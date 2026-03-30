-- CHECK Constraints: TicketTypes
ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_price CHECK (price >= 0);

ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_qty CHECK (quantity_total > 0);

ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_sold CHECK (quantity_sold >= 0);

ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_locked CHECK (quantity_locked >= 0);

ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_max_per_order CHECK (max_per_order > 0);

-- sale_end > sale_start (chỉ check khi cả 2 không NULL)
ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_sale_time
CHECK (sale_end IS NULL OR sale_start IS NULL OR sale_end > sale_start);

ALTER TABLE TicketTypes
ADD CONSTRAINT chk_tickettype_no_oversell
CHECK (quantity_sold + quantity_locked <= quantity_total);

-- ----------------------------------------------------------------
-- CHECK Constraints: Tickets
-- ----------------------------------------------------------------
ALTER TABLE Tickets
ADD CONSTRAINT chk_ticket_status
CHECK (status IN ('VALID', 'USED', 'CANCELLED', 'REFUNDED'));

-- ----------------------------------------------------------------
-- CHECK Constraints: TicketScans
-- ----------------------------------------------------------------
ALTER TABLE TicketScans
ADD CONSTRAINT chk_scan_result
CHECK (scan_result IN ('SUCCESS', 'ALREADY_USED', 'INVALID', 'CANCELLED'));

-- ----------------------------------------------------------------
-- Foreign Keys: TicketTypes
-- ----------------------------------------------------------------
ALTER TABLE TicketTypes
ADD CONSTRAINT fk_tickettypes_events
FOREIGN KEY (event_id) REFERENCES Events(event_id) ON DELETE CASCADE;

ALTER TABLE TicketTypes
ADD CONSTRAINT fk_tickettypes_sections
FOREIGN KEY (section_id) REFERENCES Sections(section_id);

-- ----------------------------------------------------------------
-- Foreign Keys: Tickets
-- ----------------------------------------------------------------
ALTER TABLE Tickets
ADD CONSTRAINT fk_tickets_orderdetails
FOREIGN KEY (detail_id) REFERENCES OrderDetails(detail_id);

ALTER TABLE Tickets
ADD CONSTRAINT fk_tickets_events
FOREIGN KEY (event_id) REFERENCES Events(event_id);

ALTER TABLE Tickets
ADD CONSTRAINT fk_tickets_customers
FOREIGN KEY (customer_id) REFERENCES Customers(customer_id);

ALTER TABLE Tickets
ADD CONSTRAINT fk_tickets_tickettypes
FOREIGN KEY (ticket_type_id) REFERENCES TicketTypes(ticket_type_id);

ALTER TABLE Tickets
ADD CONSTRAINT fk_tickets_seats
FOREIGN KEY (seat_id) REFERENCES Seats(seat_id);

-- ----------------------------------------------------------------
-- Foreign Keys: TicketScans
-- ----------------------------------------------------------------
ALTER TABLE TicketScans
ADD CONSTRAINT fk_scans_tickets
FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id);

ALTER TABLE TicketScans
ADD CONSTRAINT fk_scans_employees
FOREIGN KEY (scanned_by) REFERENCES Employees(employee_id);

-- [GIỮ NGUYÊN] FK cho event_id đã denormalize vào TicketScans
ALTER TABLE TicketScans
ADD CONSTRAINT fk_scans_events
FOREIGN KEY (event_id) REFERENCES Events(event_id);

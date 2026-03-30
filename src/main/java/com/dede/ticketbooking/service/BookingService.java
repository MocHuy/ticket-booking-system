package com.dede.ticketbooking.service;

import com.dede.ticketbooking.dto.BookingDTO.*;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final InvoiceRepository invoiceRepository;
    private final QRCodeService qrCodeService;

    @Value("${app.booking.lock-timeout-minutes}")
    private int lockTimeoutMinutes;

    /**
     * Tạo đơn hàng: chọn vé + lock ghế
     * - Kiểm tra sự kiện đang mở bán
     * - Lock ghế (optimistic locking) với timeout 10 phút
     * - Tạo order + order details
     * - Chưa tạo tickets (chỉ tạo sau khi thanh toán thành công)
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long customerId, String ipAddress, String userAgent) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!"ON_SALE".equals(event.getStatus())) {
            throw new RuntimeException("Event is not on sale");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getSaleStart()) || now.isAfter(event.getSaleEnd())) {
            throw new RuntimeException("Ticket sale is not open");
        }

        // Anti-bot: check IP rate limit (max 5 orders per hour per IP per event)
        long recentOrders = orderRepository.countRecentOrdersByIp(ipAddress, now.minusHours(1), event.getEventId());
        if (recentOrders >= 5) {
            throw new RuntimeException("Too many orders from this IP. Please try again later.");
        }

        // Generate order code
        String orderCode = "ORD-" + java.time.LocalDate.now().toString().replace("-", "") 
                          + "-" + String.format("%05d", System.nanoTime() % 100000);

        Order order = Order.builder()
                .customerId(customerId)
                .eventId(event.getEventId())
                .orderCode(orderCode)
                .status("AWAITING_PAYMENT")
                .expiredAt(now.plusMinutes(lockTimeoutMinutes))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();

        for (OrderItemRequest item : request.getItems()) {
            TicketType ticketType = ticketTypeRepository.findById(item.getTicketTypeId())
                    .orElseThrow(() -> new RuntimeException("Ticket type not found: " + item.getTicketTypeId()));

            if (!ticketType.getEventId().equals(event.getEventId())) {
                throw new RuntimeException("Ticket type does not belong to this event");
            }

            // Check quantity availability
            int available = ticketType.getQuantityTotal() - ticketType.getQuantitySold() - ticketType.getQuantityLocked();
            if (available < item.getQuantity()) {
                throw new RuntimeException("Not enough tickets available for " + ticketType.getTypeName()
                    + ". Available: " + available);
            }

            if (item.getQuantity() > ticketType.getMaxPerOrder()) {
                throw new RuntimeException("Max " + ticketType.getMaxPerOrder() + " tickets per order for " + ticketType.getTypeName());
            }

            // Lock seat if specified
            if (item.getSeatId() != null) {
                Seat seat = seatRepository.findById(item.getSeatId())
                        .orElseThrow(() -> new RuntimeException("Seat not found"));

                if (!"AVAILABLE".equals(seat.getCurrentStatus())) {
                    throw new RuntimeException("Seat " + seat.getSeatLabel() + " is not available");
                }

                seat.setCurrentStatus("LOCKED");
                seat.setLockedUntil(now.plusMinutes(lockTimeoutMinutes));
                // locked_by_order_id will be set after order save
                seatRepository.save(seat);
            }

            // Update locked quantity (optimistic locking via @Version)
            ticketType.setQuantityLocked(ticketType.getQuantityLocked() + item.getQuantity());
            ticketTypeRepository.save(ticketType);

            BigDecimal subtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            details.add(OrderDetail.builder()
                    .ticketTypeId(item.getTicketTypeId())
                    .seatId(item.getSeatId())
                    .quantity(item.getQuantity())
                    .priceSnapshot(ticketType.getPrice())
                    .subtotal(subtotal)
                    .build());
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount); // Voucher discount applied separately
        order = orderRepository.save(order);

        // Save order details
        final Long orderId = order.getOrderId();
        for (OrderDetail detail : details) {
            detail.setOrderId(orderId);
            orderDetailRepository.save(detail);

            // Update seat locked_by_order_id
            if (detail.getSeatId() != null) {
                Seat seat = seatRepository.findById(detail.getSeatId()).orElse(null);
                if (seat != null) {
                    seat.setLockedByOrderId(orderId);
                    seatRepository.save(seat);
                }
            }
        }

        // Create invoice
        invoiceRepository.save(Invoice.builder()
                .orderId(orderId)
                .customerId(customerId)
                .subTotal(totalAmount)
                .totalAmount(totalAmount)
                .build());

        log.info("Order created: {} for event {} by customer {}", orderCode, event.getEventId(), customerId);

        return buildOrderResponse(order, details);
    }

    /**
     * Sau khi thanh toán thành công → sinh tickets với QR code
     */
    @Transactional
    public List<TicketResponse> generateTickets(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order is not paid yet");
        }

        List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
        List<TicketResponse> tickets = new ArrayList<>();
        Event event = eventRepository.findById(order.getEventId()).orElse(null);

        for (OrderDetail detail : details) {
            TicketType ticketType = ticketTypeRepository.findById(detail.getTicketTypeId()).orElse(null);

            for (int i = 0; i < detail.getQuantity(); i++) {
                String ticketCode = qrCodeService.generateTicketCode();

                // Ensure uniqueness
                while (ticketRepository.existsByTicketCode(ticketCode)) {
                    ticketCode = qrCodeService.generateTicketCode();
                }

                long issuedAtEpoch = Instant.now().toEpochMilli();
                String qrCodeData = qrCodeService.generateQRCodeData(ticketCode, order.getEventId(), issuedAtEpoch);

                Ticket ticket = Ticket.builder()
                        .detailId(detail.getDetailId())
                        .eventId(order.getEventId())
                        .customerId(order.getCustomerId())
                        .ticketTypeId(detail.getTicketTypeId())
                        .seatId(detail.getSeatId())
                        .ticketCode(ticketCode)
                        .qrCodeData(qrCodeData)
                        .build();
                ticket = ticketRepository.save(ticket);

                // Mark seat as SOLD
                if (detail.getSeatId() != null) {
                    Seat seat = seatRepository.findById(detail.getSeatId()).orElse(null);
                    if (seat != null) {
                        seat.setCurrentStatus("SOLD");
                        seat.setLockedUntil(null);
                        seat.setLockedByOrderId(null);
                        seatRepository.save(seat);
                    }
                }

                tickets.add(TicketResponse.builder()
                        .ticketId(ticket.getTicketId())
                        .ticketCode(ticket.getTicketCode())
                        .qrCodeData(ticket.getQrCodeData())
                        .eventName(event != null ? event.getEventName() : "")
                        .ticketTypeName(ticketType != null ? ticketType.getTypeName() : "")
                        .seatLabel(detail.getSeatId() != null ?
                            seatRepository.findById(detail.getSeatId()).map(Seat::getSeatLabel).orElse("") : "Standing")
                        .holderName(ticket.getHolderName())
                        .status(ticket.getStatus())
                        .issuedAt(ticket.getIssuedAt().toString())
                        .build());
            }

            // Update sold quantity
            if (ticketType != null) {
                ticketType.setQuantitySold(ticketType.getQuantitySold() + detail.getQuantity());
                ticketType.setQuantityLocked(ticketType.getQuantityLocked() - detail.getQuantity());
                ticketTypeRepository.save(ticketType);
            }
        }

        log.info("Generated {} tickets for order {}", tickets.size(), orderId);
        return tickets;
    }

    /**
     * Hủy đơn hàng: giải phóng ghế + giảm quantity_locked
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("PAID".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
        for (OrderDetail detail : details) {
            // Release seat
            if (detail.getSeatId() != null) {
                Seat seat = seatRepository.findById(detail.getSeatId()).orElse(null);
                if (seat != null && "LOCKED".equals(seat.getCurrentStatus())) {
                    seat.setCurrentStatus("AVAILABLE");
                    seat.setLockedUntil(null);
                    seat.setLockedByOrderId(null);
                    seatRepository.save(seat);
                }
            }

            // Reduce locked quantity
            TicketType ticketType = ticketTypeRepository.findById(detail.getTicketTypeId()).orElse(null);
            if (ticketType != null) {
                ticketType.setQuantityLocked(Math.max(0, ticketType.getQuantityLocked() - detail.getQuantity()));
                ticketTypeRepository.save(ticketType);
            }
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
        return buildOrderResponse(order, details);
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderDetail> details) {
        List<OrderDetailResponse> detailResponses = details.stream().map(d -> {
            TicketType tt = ticketTypeRepository.findById(d.getTicketTypeId()).orElse(null);
            String seatLabel = d.getSeatId() != null ?
                    seatRepository.findById(d.getSeatId()).map(Seat::getSeatLabel).orElse("") : "Standing";
            return OrderDetailResponse.builder()
                    .detailId(d.getDetailId())
                    .ticketTypeName(tt != null ? tt.getTypeName() : "")
                    .seatLabel(seatLabel)
                    .quantity(d.getQuantity())
                    .priceSnapshot(d.getPriceSnapshot())
                    .subtotal(d.getSubtotal())
                    .build();
        }).toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .expiredAt(order.getExpiredAt() != null ? order.getExpiredAt().toString() : null)
                .details(detailResponses)
                .build();
    }
}

package com.dede.ticketbooking.service;

import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled Jobs — Dọn dẹp tự động
 * 
 * 1. Giải phóng ghế hết hạn lock (mỗi 1 phút)
 * 2. Hủy order hết hạn thanh toán (mỗi 1 phút)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobService {

    private final SeatRepository seatRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Giải phóng ghế LOCKED đã hết hạn
     * Chạy mỗi 60 giây
     */
    @Scheduled(fixedRateString = "${app.booking.cleanup-interval-ms}")
    @Transactional
    public void releaseExpiredSeats() {
        List<Seat> expiredSeats = seatRepository.findExpiredLockedSeats(LocalDateTime.now());
        
        for (Seat seat : expiredSeats) {
            seat.setCurrentStatus("AVAILABLE");
            seat.setLockedUntil(null);
            seat.setLockedByOrderId(null);
            seatRepository.save(seat);
        }

        if (!expiredSeats.isEmpty()) {
            log.info("Released {} expired locked seats", expiredSeats.size());
        }
    }

    /**
     * Hủy order hết hạn thanh toán
     * Chạy mỗi 60 giây
     */
    @Scheduled(fixedRateString = "${app.booking.cleanup-interval-ms}")
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findExpiredOrders(LocalDateTime.now());

        for (Order order : expiredOrders) {
            // Release seats and reduce locked quantities
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getOrderId());
            for (OrderDetail detail : details) {
                if (detail.getSeatId() != null) {
                    Seat seat = seatRepository.findById(detail.getSeatId()).orElse(null);
                    if (seat != null && "LOCKED".equals(seat.getCurrentStatus())) {
                        seat.setCurrentStatus("AVAILABLE");
                        seat.setLockedUntil(null);
                        seat.setLockedByOrderId(null);
                        seatRepository.save(seat);
                    }
                }

                TicketType ticketType = ticketTypeRepository.findById(detail.getTicketTypeId()).orElse(null);
                if (ticketType != null) {
                    ticketType.setQuantityLocked(Math.max(0, ticketType.getQuantityLocked() - detail.getQuantity()));
                    ticketTypeRepository.save(ticketType);
                }
            }

            order.setStatus("CANCELLED");
            order.setNote("Auto-cancelled: payment timeout");
            orderRepository.save(order);

            log.info("Auto-cancelled expired order: {}", order.getOrderCode());
        }

        if (!expiredOrders.isEmpty()) {
            log.info("Cancelled {} expired orders", expiredOrders.size());
        }
    }
}

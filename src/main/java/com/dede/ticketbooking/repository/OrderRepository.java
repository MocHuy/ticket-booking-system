package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByCustomerIdAndIsDeleted(Long customerId, Integer isDeleted);

    List<Order> findByEventIdAndStatus(Long eventId, String status);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'AWAITING_PAYMENT') AND o.expiredAt < :now")
    List<Order> findExpiredOrders(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.eventId = :eventId AND o.status = 'PAID' AND o.isDeleted = 0")
    long countPaidOrdersByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.eventId = :eventId AND o.status = 'PAID' AND o.isDeleted = 0")
    java.math.BigDecimal sumRevenueByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.ipAddress = :ip AND o.createdAt > :since AND o.eventId = :eventId")
    long countRecentOrdersByIp(@Param("ip") String ip, @Param("since") LocalDateTime since, @Param("eventId") Long eventId);
}

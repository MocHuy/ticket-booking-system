package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByEventIdAndStatus(Long eventId, String status);

    List<Ticket> findByDetailId(Long detailId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.eventId = :eventId AND t.status = 'VALID'")
    long countValidTicketsByEvent(@Param("eventId") Long eventId);

    @Query("SELECT t FROM Ticket t WHERE t.eventId = :eventId AND t.status IN ('VALID', 'USED')")
    List<Ticket> findActiveTicketsByEvent(@Param("eventId") Long eventId);

    boolean existsByTicketCode(String ticketCode);

    List<Ticket> findByCustomerIdAndStatus(Long customerId, String status);
}

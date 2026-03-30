package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.TicketScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TicketScanRepository extends JpaRepository<TicketScan, Long> {
    List<TicketScan> findByTicketId(Long ticketId);

    @Query("SELECT COUNT(ts) FROM TicketScan ts WHERE ts.eventId = :eventId AND ts.scanResult = 'SUCCESS'")
    long countSuccessfulScansByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(ts) FROM TicketScan ts WHERE ts.eventId = :eventId")
    long countTotalScansByEvent(@Param("eventId") Long eventId);
}

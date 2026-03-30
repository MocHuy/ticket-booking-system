package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySectionIdAndIsActive(Long sectionId, Integer isActive);

    @Query("SELECT s FROM Seat s WHERE s.sectionId = :sectionId AND s.currentStatus = 'AVAILABLE' AND s.isActive = 1")
    List<Seat> findAvailableSeats(@Param("sectionId") Long sectionId);

    @Query("SELECT s FROM Seat s WHERE s.currentStatus = 'LOCKED' AND s.lockedUntil < :now")
    List<Seat> findExpiredLockedSeats(@Param("now") LocalDateTime now);

    Optional<Seat> findBySeatIdAndCurrentStatus(Long seatId, String status);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.sectionId = :sectionId AND s.currentStatus = :status")
    long countBySectionIdAndStatus(@Param("sectionId") Long sectionId, @Param("status") String status);
}

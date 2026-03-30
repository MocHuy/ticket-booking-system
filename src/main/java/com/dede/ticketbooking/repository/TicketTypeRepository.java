package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    List<TicketType> findByEventIdAndIsActive(Long eventId, Integer isActive);
}

package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByEventId(Long eventId);
}

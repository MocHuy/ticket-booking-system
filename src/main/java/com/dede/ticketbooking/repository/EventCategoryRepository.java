package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {
}

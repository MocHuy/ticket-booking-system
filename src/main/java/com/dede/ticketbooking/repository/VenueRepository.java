package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByIsActive(Integer isActive);
    List<Venue> findByCity(String city);
}

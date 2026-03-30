package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByIsDeletedAndStatusIn(Integer isDeleted, List<String> statuses, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isDeleted = 0 AND e.status = 'ON_SALE' " +
           "AND e.saleStart <= :now AND e.saleEnd >= :now")
    List<Event> findOnSaleEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.isDeleted = 0 AND e.category.categoryId = :categoryId")
    Page<Event> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    List<Event> findByVenueVenueIdAndIsDeleted(Long venueId, Integer isDeleted);
}

package com.dede.ticketbooking.service;

import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public Page<Event> getPublishedEvents(Pageable pageable) {
        return eventRepository.findByIsDeletedAndStatusIn(0, 
                List.of("PUBLISHED", "ON_SALE", "SOLD_OUT"), pageable);
    }

    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }

    @Transactional
    public Event createEvent(Event event) {
        // Validate venue exists
        venueRepository.findById(event.getVenue().getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long eventId, Event updated) {
        Event event = getEvent(eventId);
        event.setEventName(updated.getEventName());
        event.setDescription(updated.getDescription());
        event.setBannerUrl(updated.getBannerUrl());
        event.setStartDatetime(updated.getStartDatetime());
        event.setEndDatetime(updated.getEndDatetime());
        event.setSaleStart(updated.getSaleStart());
        event.setSaleEnd(updated.getSaleEnd());
        event.setStatus(updated.getStatus());
        return eventRepository.save(event);
    }

    public List<Section> getSections(Long eventId) {
        return sectionRepository.findByEventId(eventId);
    }

    @Transactional
    public Section createSection(Section section) {
        Section saved = sectionRepository.save(section);

        // Auto-generate seats for SEATED sections
        if ("SEATED".equals(section.getSectionType())) {
            generateSeats(saved);
        }

        return saved;
    }

    public List<Seat> getSeats(Long sectionId) {
        return seatRepository.findBySectionIdAndIsActive(sectionId, 1);
    }

    public List<Seat> getAvailableSeats(Long sectionId) {
        return seatRepository.findAvailableSeats(sectionId);
    }

    public List<TicketType> getTicketTypes(Long eventId) {
        return ticketTypeRepository.findByEventIdAndIsActive(eventId, 1);
    }

    @Transactional
    public TicketType createTicketType(TicketType ticketType) {
        return ticketTypeRepository.save(ticketType);
    }

    public List<EventCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Venue> getAllVenues() {
        return venueRepository.findByIsActive(1);
    }

    private void generateSeats(Section section) {
        int totalSeats = section.getTotalSeats();
        int cols = 10; // seats per row
        int rows = (int) Math.ceil((double) totalSeats / cols);

        int count = 0;
        for (int r = 0; r < rows && count < totalSeats; r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            for (int c = 1; c <= cols && count < totalSeats; c++) {
                String seatLabel = rowLabel + String.format("%02d", c);
                seatRepository.save(Seat.builder()
                        .sectionId(section.getSectionId())
                        .seatLabel(seatLabel)
                        .seatRow(rowLabel)
                        .seatCol(String.valueOf(c))
                        .build());
                count++;
            }
        }
    }
}

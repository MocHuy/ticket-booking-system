package com.dede.ticketbooking.controller;

import com.dede.ticketbooking.dto.ApiResponse;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Event>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Event> events = eventService.getPublishedEvents(
                PageRequest.of(page, size, Sort.by("startDatetime").descending()));
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Event>> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEvent(eventId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Event>> createEvent(@RequestBody Event event) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo sự kiện thành công", eventService.createEvent(event)));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Event>> updateEvent(@PathVariable Long eventId, @RequestBody Event event) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sự kiện thành công", eventService.updateEvent(eventId, event)));
    }

    @GetMapping("/{eventId}/sections")
    public ResponseEntity<ApiResponse<List<Section>>> getSections(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getSections(eventId)));
    }

    @PostMapping("/{eventId}/sections")
    public ResponseEntity<ApiResponse<Section>> createSection(@PathVariable Long eventId, @RequestBody Section section) {
        section.setEventId(eventId);
        return ResponseEntity.ok(ApiResponse.ok("Tạo khu vực thành công", eventService.createSection(section)));
    }

    @GetMapping("/sections/{sectionId}/seats")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeats(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getSeats(sectionId)));
    }

    @GetMapping("/sections/{sectionId}/seats/available")
    public ResponseEntity<ApiResponse<List<Seat>>> getAvailableSeats(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getAvailableSeats(sectionId)));
    }

    @GetMapping("/{eventId}/ticket-types")
    public ResponseEntity<ApiResponse<List<TicketType>>> getTicketTypes(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getTicketTypes(eventId)));
    }

    @PostMapping("/{eventId}/ticket-types")
    public ResponseEntity<ApiResponse<TicketType>> createTicketType(@PathVariable Long eventId, @RequestBody TicketType ticketType) {
        ticketType.setEventId(eventId);
        return ResponseEntity.ok(ApiResponse.ok("Tạo loại vé thành công", eventService.createTicketType(ticketType)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<EventCategory>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getAllCategories()));
    }

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<Venue>>> getVenues() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getAllVenues()));
    }
}

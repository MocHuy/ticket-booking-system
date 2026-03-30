package com.dede.ticketbooking.service;

import com.dede.ticketbooking.dto.ReportDTO.*;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final TicketScanRepository ticketScanRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public EventReportResponse getEventReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        List<TicketType> ticketTypes = ticketTypeRepository.findByEventIdAndIsActive(eventId, 1);

        long ticketsSold = ticketTypes.stream().mapToInt(TicketType::getQuantitySold).sum();
        long ticketsScanned = ticketScanRepository.countSuccessfulScansByEvent(eventId);
        long totalScans = ticketScanRepository.countTotalScansByEvent(eventId);
        BigDecimal totalRevenue = orderRepository.sumRevenueByEvent(eventId);

        List<TicketTypeReport> breakdown = ticketTypes.stream().map(tt ->
            TicketTypeReport.builder()
                    .typeName(tt.getTypeName())
                    .quantityTotal(tt.getQuantityTotal())
                    .quantitySold(tt.getQuantitySold())
                    .quantityLocked(tt.getQuantityLocked())
                    .revenue(tt.getPrice().multiply(BigDecimal.valueOf(tt.getQuantitySold())))
                    .build()
        ).toList();

        return EventReportResponse.builder()
                .eventId(eventId)
                .eventName(event.getEventName())
                .totalCapacity(event.getTotalCapacity())
                .ticketsSold(ticketsSold)
                .ticketsScanned(ticketsScanned)
                .totalRevenue(totalRevenue)
                .scanSuccessRate(totalScans > 0 ? (double) ticketsScanned / totalScans * 100 : 0)
                .ticketTypeBreakdown(breakdown)
                .build();
    }

    public SystemStatsResponse getSystemStats() {
        long totalEvents = eventRepository.count();
        long totalTicketsSold = ticketRepository.count();
        long successfulPayments = paymentRepository.countSuccessfulPayments();
        long totalPayments = paymentRepository.countTotalPayments();
        long totalUsers = userRepository.count();

        // Calculate total revenue across all events
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Event event : eventRepository.findAll()) {
            totalRevenue = totalRevenue.add(orderRepository.sumRevenueByEvent(event.getEventId()));
        }

        return SystemStatsResponse.builder()
                .totalEvents(totalEvents)
                .totalTicketsSold(totalTicketsSold)
                .totalRevenue(totalRevenue)
                .paymentSuccessRate(totalPayments > 0 ? (double) successfulPayments / totalPayments * 100 : 0)
                .totalUsers(totalUsers)
                .build();
    }
}

package com.dede.ticketbooking.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

public class ReportDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EventReportResponse {
        private Long eventId;
        private String eventName;
        private int totalCapacity;
        private long ticketsSold;
        private long ticketsScanned;
        private BigDecimal totalRevenue;
        private double scanSuccessRate;
        private List<TicketTypeReport> ticketTypeBreakdown;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TicketTypeReport {
        private String typeName;
        private int quantityTotal;
        private int quantitySold;
        private int quantityLocked;
        private BigDecimal revenue;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SystemStatsResponse {
        private long totalEvents;
        private long totalTicketsSold;
        private BigDecimal totalRevenue;
        private double paymentSuccessRate;
        private long totalUsers;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScanReportResponse {
        private Long eventId;
        private String eventName;
        private long totalScans;
        private long successfulScans;
        private long alreadyUsedScans;
        private long invalidScans;
        private double successRate;
    }
}

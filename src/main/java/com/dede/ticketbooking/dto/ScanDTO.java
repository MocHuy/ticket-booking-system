package com.dede.ticketbooking.dto;

import lombok.*;

public class ScanDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ScanRequest {
        private String ticketCode;
        private Long scannedBy;     // employee_id
        private Long eventId;
        private String scanLocation;
        private String deviceId;
        private boolean offlineScan;
        private String scannedAt;   // ISO timestamp (for offline scans)
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScanResponse {
        private String result;   // SUCCESS, ALREADY_USED, INVALID, CANCELLED
        private String message;
        private String ticketCode;
        private String holderName;
        private String ticketType;
        private String seatLabel;
        private String eventName;
        private long processingTimeMs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OfflineSyncRequest {
        private java.util.List<ScanRequest> scans;
    }
}

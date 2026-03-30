package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TicketScans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scan_id")
    private Long scanId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "scanned_by", nullable = false)
    private Long scannedBy;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "scan_result", nullable = false, length = 20)
    private String scanResult;

    @Column(name = "scan_location", length = 100)
    private String scanLocation;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Column(name = "synced_at")
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();

    @Column(name = "is_offline_scan")
    @Builder.Default
    private Integer isOfflineScan = 0;
}

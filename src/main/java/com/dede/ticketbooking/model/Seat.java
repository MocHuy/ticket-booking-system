package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Seats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "seat_label"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "seat_label", nullable = false, length = 20)
    private String seatLabel;

    @Column(name = "seat_row", length = 10)
    private String seatRow;

    @Column(name = "seat_col", length = 10)
    private String seatCol;

    @Column(name = "current_status", length = 20)
    @Builder.Default
    private String currentStatus = "AVAILABLE";

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "locked_by_order_id")
    private Long lockedByOrderId;

    @Column(name = "is_active")
    @Builder.Default
    private Integer isActive = 1;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}

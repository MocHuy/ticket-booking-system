package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Sections",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "section_name"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "color_code", length = 10)
    @Builder.Default
    private String colorCode = "#CCCCCC";

    @Column(name = "section_type", length = 20)
    @Builder.Default
    private String sectionType = "SEATED";

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

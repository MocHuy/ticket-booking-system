package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TicketTypes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;

    @Column(name = "quantity_sold")
    @Builder.Default
    private Integer quantitySold = 0;

    @Column(name = "quantity_locked")
    @Builder.Default
    private Integer quantityLocked = 0;

    @Column(name = "sale_start")
    private LocalDateTime saleStart;

    @Column(name = "sale_end")
    private LocalDateTime saleEnd;

    @Column(name = "max_per_order")
    @Builder.Default
    private Integer maxPerOrder = 4;

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
    private Long version;
}

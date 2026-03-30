package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Tickets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "detail_id", nullable = false)
    private Long detailId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "ticket_type_id", nullable = false)
    private Long ticketTypeId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "ticket_code", unique = true, nullable = false, length = 100)
    private String ticketCode;

    @Column(name = "qr_code_data", nullable = false, length = 500)
    private String qrCodeData;

    @Column(name = "holder_name", length = 100)
    private String holderName;

    @Column(name = "holder_email", length = 150)
    private String holderEmail;

    @Column(name = "holder_phone", length = 20)
    private String holderPhone;

    @Column(length = 20)
    @Builder.Default
    private String status = "VALID";

    @Column(name = "issued_at", updatable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}

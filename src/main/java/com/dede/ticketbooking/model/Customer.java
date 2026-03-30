package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "membership_tier_id")
    private Long membershipTierId;

    @Column(name = "customer_code", unique = true, length = 50)
    private String customerCode;

    @Column(name = "loyalty_points")
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Column(name = "lifetime_spending", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal lifetimeSpending = java.math.BigDecimal.ZERO;

    @Column(name = "company_info", length = 255)
    private String companyInfo;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_deleted")
    @Builder.Default
    private Integer isDeleted = 0;
}

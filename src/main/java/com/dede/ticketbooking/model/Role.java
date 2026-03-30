package com.dede.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", unique = true, nullable = false, length = 100)
    private String roleName;

    @Column(length = 255)
    private String description;

    @Column(name = "is_system_role")
    @Builder.Default
    private Integer isSystemRole = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Integer isActive = 1;

    @Column(name = "is_deleted")
    @Builder.Default
    private Integer isDeleted = 0;
}

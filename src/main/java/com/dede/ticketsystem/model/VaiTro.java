package com.dede.ticketsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "VAITRO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VaiTro {

    @Id
    @Column(name = "MaVaiTro", length = 50)
    private String maVaiTro;

    @Column(name = "TenVaiTro", length = 100)
    private String tenVaiTro;

    @Column(name = "MoTa", length = 255)
    private String moTa;
}
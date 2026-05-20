package com.dede.ticketsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHITIETVAITRO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ChiTietVaiTroID.class)
public class ChiTietVaiTro {

    @Id
    @Column(name = "MaND", length = 50)
    private String maND;

    @Id
    @Column(name = "MaVaiTro", length = 50)
    private String maVaiTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaND", insertable = false, updatable = false)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaVaiTro", insertable = false, updatable = false)
    private VaiTro vaiTro;
}
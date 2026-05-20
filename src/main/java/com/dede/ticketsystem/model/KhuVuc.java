package com.dede.ticketsystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "KHUVUC")
public class KhuVuc {

    @Id
    @Column(name = "MaKhuVuc", length = 50)
    private String maKhuVuc;

    @Column(name = "TenKhuVuc", length = 100)
    private String tenKhuVuc;

    @Column(name = "GiaVe", precision = 18, scale = 2)
    private BigDecimal giaVe;

    @Column(name = "SoHang")
    private Integer soHang;

    @Column(name = "SoGheMoiHang")
    private Integer soGheMoiHang;

    @Column(name = "MauSac", length = 20)
    private String mauSac;

    @Column(name = "MaSK", length = 50)
    private String maSK;

    // Constructors
    public KhuVuc() {}

    public KhuVuc(String maKhuVuc, String tenKhuVuc, BigDecimal giaVe, Integer soHang, Integer soGheMoiHang, String mauSac, String maSK) {
        this.maKhuVuc = maKhuVuc;
        this.tenKhuVuc = tenKhuVuc;
        this.giaVe = giaVe;
        this.soHang = soHang;
        this.soGheMoiHang = soGheMoiHang;
        this.mauSac = mauSac;
        this.maSK = maSK;
    }

    // Getters and Setters
    public String getMaKhuVuc() { return maKhuVuc; }
    public void setMaKhuVuc(String maKhuVuc) { this.maKhuVuc = maKhuVuc; }

    public String getTenKhuVuc() { return tenKhuVuc; }
    public void setTenKhuVuc(String tenKhuVuc) { this.tenKhuVuc = tenKhuVuc; }

    public BigDecimal getGiaVe() { return giaVe; }
    public void setGiaVe(BigDecimal giaVe) { this.giaVe = giaVe; }

    public Integer getSoHang() { return soHang; }
    public void setSoHang(Integer soHang) { this.soHang = soHang; }

    public Integer getSoGheMoiHang() { return soGheMoiHang; }
    public void setSoGheMoiHang(Integer soGheMoiHang) { this.soGheMoiHang = soGheMoiHang; }

    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }

    public String getMaSK() { return maSK; }
    public void setMaSK(String maSK) { this.maSK = maSK; }
}

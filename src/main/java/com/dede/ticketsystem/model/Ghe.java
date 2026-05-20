package com.dede.ticketsystem.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "GHE")
public class Ghe {

    @Id
    @Column(name = "MaGhe", length = 50)
    private String maGhe;

    @Column(name = "TenGhe", length = 50)
    private String tenGhe;

    @Column(name = "TrangThai", length = 50)
    private String trangThai; // Trống, Đã đặt, Đang giữ

    @Column(name = "MaKhuVuc", length = 50)
    private String maKhuVuc;

    @Column(name = "MaSK", length = 50)
    private String maSK;

    @Column(name = "ThoiGianGiu")
    private Timestamp thoiGianGiu;

    @Column(name = "MaKHDangGiu", length = 50)
    private String maKHDangGiu;

    // Constructors
    public Ghe() {}

    public Ghe(String maGhe, String tenGhe, String trangThai, String maKhuVuc, String maSK) {
        this.maGhe = maGhe;
        this.tenGhe = tenGhe;
        this.trangThai = trangThai;
        this.maKhuVuc = maKhuVuc;
        this.maSK = maSK;
    }

    // Getters and Setters
    public String getMaGhe() { return maGhe; }
    public void setMaGhe(String maGhe) { this.maGhe = maGhe; }

    public String getTenGhe() { return tenGhe; }
    public void setTenGhe(String tenGhe) { this.tenGhe = tenGhe; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getMaKhuVuc() { return maKhuVuc; }
    public void setMaKhuVuc(String maKhuVuc) { this.maKhuVuc = maKhuVuc; }

    public String getMaSK() { return maSK; }
    public void setMaSK(String maSK) { this.maSK = maSK; }

    public Timestamp getThoiGianGiu() { return thoiGianGiu; }
    public void setThoiGianGiu(Timestamp thoiGianGiu) { this.thoiGianGiu = thoiGianGiu; }

    public String getMaKHDangGiu() { return maKHDangGiu; }
    public void setMaKHDangGiu(String maKHDangGiu) { this.maKHDangGiu = maKHDangGiu; }
}

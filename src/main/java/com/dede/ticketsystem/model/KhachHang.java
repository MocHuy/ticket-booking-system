package com.dede.ticketsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "KHACHHANG")
public class KhachHang {

    @Id
    @Column(name = "MAKH", length = 50)
    private String maKH;

    @Column(name = "HOTEN")
    private String hoTen;

    @Column(name = "SDT")
    private String sdt;

    @Column(name = "EMAIL")
    private String email;

    @OneToOne
    @JoinColumn(name = "MAND")
    private NguoiDung nguoiDung;

    public KhachHang() {
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }
}

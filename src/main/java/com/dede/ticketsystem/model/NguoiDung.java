package com.dede.ticketsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "NGUOIDUNG")
public class NguoiDung {

    @Id
    @Column(name = "MAND", length = 50)
    private String maND;

    @Column(name = "TENTAIKHOAN", unique = true, nullable = false)
    private String tenTaiKhoan;

    @Column(name = "MATKHAUMAHOA", nullable = false)
    private String matKhauMaHoa;

    @Column(name = "HOTEN")
    private String hoTen;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "SDT")
    private String sdt;

    @Column(name = "MAVT")
    private String maVT;

    @Column(name = "TRANGTHAINDU")
    private String trangThaiND;

    public NguoiDung() {
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.maVT) || "VT01".equals(this.maVT);
    }

    public String getMaND() {
        return maND;
    }

    public void setMaND(String maND) {
        this.maND = maND;
    }

    public String getTenTaiKhoan() {
        return tenTaiKhoan;
    }

    public void setTenTaiKhoan(String tenTaiKhoan) {
        this.tenTaiKhoan = tenTaiKhoan;
    }

    public String getMatKhauMaHoa() {
        return matKhauMaHoa;
    }

    public void setMatKhauMaHoa(String matKhauMaHoa) {
        this.matKhauMaHoa = matKhauMaHoa;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public String getMaVT() {
        return maVT;
    }

    public void setMaVT(String maVT) {
        this.maVT = maVT;
    }

    public String getTrangThaiND() {
        return trangThaiND;
    }

    public void setTrangThaiND(String trangThaiND) {
        this.trangThaiND = trangThaiND;
    }
}

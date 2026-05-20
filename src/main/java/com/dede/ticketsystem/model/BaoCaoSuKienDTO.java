package com.dede.ticketsystem.model;

import java.math.BigDecimal;

public class BaoCaoSuKienDTO {
    private String maSK;
    private String tenSK;
    private int tongSoVe;
    private int veDaBan;
    private int veConLai;
    private BigDecimal doanhThu;
    private double tyLeLapDay;
    private long soLuotXem;
    private long soLuotClickDatVe;
    private long soLuotBoGio;
    private double tyLeChuyenDoi;

    public BaoCaoSuKienDTO() {
    }

    public String getMaSK() {
        return maSK;
    }

    public void setMaSK(String maSK) {
        this.maSK = maSK;
    }

    public String getTenSK() {
        return tenSK;
    }

    public void setTenSK(String tenSK) {
        this.tenSK = tenSK;
    }

    public int getTongSoVe() {
        return tongSoVe;
    }

    public void setTongSoVe(int tongSoVe) {
        this.tongSoVe = tongSoVe;
    }

    public int getVeDaBan() {
        return veDaBan;
    }

    public void setVeDaBan(int veDaBan) {
        this.veDaBan = veDaBan;
    }

    public int getVeConLai() {
        return veConLai;
    }

    public void setVeConLai(int veConLai) {
        this.veConLai = veConLai;
    }

    public BigDecimal getDoanhThu() {
        return doanhThu;
    }

    public void setDoanhThu(BigDecimal doanhThu) {
        this.doanhThu = doanhThu;
    }

    public double getTyLeLapDay() {
        return tyLeLapDay;
    }

    public void setTyLeLapDay(double tyLeLapDay) {
        this.tyLeLapDay = tyLeLapDay;
    }

    public long getSoLuotXem() {
        return soLuotXem;
    }

    public void setSoLuotXem(long soLuotXem) {
        this.soLuotXem = soLuotXem;
    }

    public long getSoLuotClickDatVe() {
        return soLuotClickDatVe;
    }

    public void setSoLuotClickDatVe(long soLuotClickDatVe) {
        this.soLuotClickDatVe = 
        soLuotClickDatVe;
    }

    public long getSoLuotBoGio() {
        return soLuotBoGio;
    }

    public void setSoLuotBoGio(long soLuotBoGio) {
        this.soLuotBoGio = soLuotBoGio;
    }

    public double getTyLeChuyenDoi() {
        return tyLeChuyenDoi;
    }

    public void setTyLeChuyenDoi(double tyLeChuyenDoi) {
        this.tyLeChuyenDoi = tyLeChuyenDoi;
    }
}

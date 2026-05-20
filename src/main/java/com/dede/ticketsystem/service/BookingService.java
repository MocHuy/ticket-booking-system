package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.model.DonHang;
import com.dede.ticketsystem.model.Ve;
import com.dede.ticketsystem.repository.GheRepository;
import com.dede.ticketsystem.repository.DonHangRepository;
import com.dede.ticketsystem.repository.VeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class BookingService {

    private final GheRepository gheRepository;
    private final DonHangRepository donHangRepository;
    private final VeRepository veRepository;

    public BookingService(GheRepository gheRepository, DonHangRepository donHangRepository, VeRepository veRepository) {
        this.gheRepository = gheRepository;
        this.donHangRepository = donHangRepository;
        this.veRepository = veRepository;
    }

    @Transactional
    public void lockSeats(List<String> maGheList, String maKH) {
        List<Ghe> gheList = gheRepository.findAllById(maGheList);
        
        for (Ghe ghe : gheList) {
            if (!"Trống".equals(ghe.getTrangThai())) {
                throw new RuntimeException("Ghế " + ghe.getTenGhe() + " đã được đặt hoặc đang được giữ bởi người khác!");
            }
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Ghe ghe : gheList) {
            ghe.setTrangThai("Đang giữ");
            ghe.setThoiGianGiu(now);
            ghe.setMaKHDangGiu(maKH);
        }

        gheRepository.saveAll(gheList);
    }

    @Transactional
    public void processCheckout(List<String> maGheList, boolean success, String maKH) {
        List<Ghe> gheList = gheRepository.findAllById(maGheList);
        
        if (!success) {
            // Thanh toán thất bại -> Nhả ghế
            for (Ghe ghe : gheList) {
                if ("Đang giữ".equals(ghe.getTrangThai())) {
                    ghe.setTrangThai("Trống");
                    ghe.setThoiGianGiu(null);
                    ghe.setMaKHDangGiu(null);
                }
            }
            gheRepository.saveAll(gheList);
            return;
        }

        // Thanh toán thành công -> Đặt ghế, tạo đơn hàng, cập nhật vé
        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        
        DonHang dh = new DonHang();
        String maDon = "DH-" + System.currentTimeMillis();
        dh.setMaDonHang(maDon);
        dh.setSoDonHang(maDon);
        dh.setTrangThaiDonHang("Đã thanh toán"); // Đã thanh toán
        dh.setThoiGianDat(new java.sql.Timestamp(System.currentTimeMillis()));
        dh.setMaKH(maKH);
        
        // Cập nhật ghế
        for (Ghe ghe : gheList) {
            ghe.setTrangThai("Đã đặt");
            ghe.setThoiGianGiu(null); // Clear hold timer
            
            // Tìm vé tương ứng với ghế này
            Ve ve = veRepository.findByMaGhe(ghe.getMaGhe());
            if (ve != null) {
                ve.setMaDonHang(maDon);
                ve.setTrangThaiVe("Đã bán");
                veRepository.save(ve);
                if (ve.getGiaVe() != null) {
                    tongTien = tongTien.add(ve.getGiaVe());
                }
            }
        }
        gheRepository.saveAll(gheList);
        
        dh.setTongTien(tongTien);
        dh.setThanhTien(tongTien);
        donHangRepository.save(dh);
    }
}

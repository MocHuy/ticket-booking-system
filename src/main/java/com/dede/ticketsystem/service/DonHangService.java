package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.DonHang;
import com.dede.ticketsystem.model.Ve;
import com.dede.ticketsystem.repository.DonHangRepository;
import com.dede.ticketsystem.repository.VeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DonHangService {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private VeRepository veRepository;

    public List<DonHang> layTatCa() {
        return donHangRepository.findAll();
    }

    public List<DonHang> timKiem(String keyword, String trangThai) {
        return donHangRepository.search(keyword, trangThai);
    }

    public Optional<DonHang> timTheoMa(String maDonHang) {
        return donHangRepository.findById(maDonHang);
    }

    public void huyDonHang(String maDonHang) {
        donHangRepository.findById(maDonHang).ifPresent(dh -> {
            dh.setTrangThaiDonHang("Đã hủy");
            dh.setCapNhatLanCuoi(new java.sql.Timestamp(System.currentTimeMillis()));
            donHangRepository.save(dh);
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public DonHang taoDonHang(String maSK, int soLuong, String maKH, String maNV) {
        List<Ve> veTrong = veRepository.findAvailableTickets(maSK, "-", org.springframework.data.domain.PageRequest.of(0, soLuong));
        
        if (veTrong.size() < soLuong) {
            throw new RuntimeException("Không đủ số lượng vé trống cho sự kiện này. Chỉ còn " + veTrong.size() + " vé.");
        }
        
        DonHang dh = new DonHang();
        String maDon = "DH-" + System.currentTimeMillis();
        dh.setMaDonHang(maDon);
        dh.setSoDonHang(maDon);
        dh.setTrangThaiDonHang("Chờ thanh toán");
        dh.setThoiGianDat(new java.sql.Timestamp(System.currentTimeMillis()));
        dh.setMaKH(maKH);
        dh.setMaNV(maNV);
        
        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        for (Ve v : veTrong) {
            if (v.getGiaVe() != null) {
                tongTien = tongTien.add(v.getGiaVe());
            }
        }
        dh.setTongTien(tongTien);
        dh.setThanhTien(tongTien);
        
        DonHang savedDh = donHangRepository.save(dh);
        
        for (Ve v : veTrong) {
            v.setMaDonHang(savedDh.getMaDonHang());
            v.setTrangThaiVe("Đã bán");
        }
        veRepository.saveAll(veTrong);
        
        // Update SoVeDaBan in SuKien (optional but recommended, can be done via SuKienRepository or just trust the Ticket count)
        
        return savedDh;
    }
}

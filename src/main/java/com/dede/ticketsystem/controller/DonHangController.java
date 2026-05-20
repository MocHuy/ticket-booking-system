package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.DonHang;
import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.service.DonHangService;
import com.dede.ticketsystem.service.TaiKhoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/donhang")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;

    @Autowired
    private TaiKhoanService taiKhoanService;

    /** Trang danh sách đơn hàng */
    @GetMapping
    public String danhSach(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String trangThai,
            Model model) {
        List<DonHang> list;
        try {
            list = donHangService.timKiem(keyword, trangThai);
        } catch (Exception e) {
            list = new java.util.ArrayList<>();
        }

        Map<String, String> mapKhachHang = new java.util.HashMap<>();
        try {
            List<NguoiDung> nguoiDungs = taiKhoanService.getDanhSachTatCa();
            for (NguoiDung nd : nguoiDungs) {
                mapKhachHang.put(nd.getMaND(), nd.getTenTaiKhoan());
            }
        } catch (Exception e) {}

        model.addAttribute("donHangList", list);
        model.addAttribute("mapKhachHang", mapKhachHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThaiFilter", trangThai);
        return "QLDH/QLDH";
    }

    /** API: lấy chi tiết 1 đơn hàng theo mã */
    @GetMapping("/api/{maDonHang}")
    @ResponseBody
    public ResponseEntity<?> chiTiet(@PathVariable String maDonHang) {
        return donHangService.timTheoMa(maDonHang)
                .map(dh -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("maDonHang", dh.getMaDonHang());
                    data.put("soDonHang", dh.getSoDonHang());
                    data.put("tongTien", dh.getTongTien());
                    data.put("thanhTien", dh.getThanhTien());
                    data.put("trangThaiDonHang", dh.getTrangThaiDonHang());
                    data.put("thoiGianDat", dh.getThoiGianDat() != null ? dh.getThoiGianDat().toString() : null);
                    data.put("thoiGianHetHan", dh.getThoiGianHetHan() != null ? dh.getThoiGianHetHan().toString() : null);
                    data.put("maKH", dh.getMaKH());
                    data.put("maNV", dh.getMaNV());
                    data.put("maPGG", dh.getMaPGG());
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** API: hủy đơn hàng */
    @PostMapping("/huy/{maDonHang}")
    @ResponseBody
    public ResponseEntity<?> huyDon(@PathVariable String maDonHang) {
        try {
            donHangService.huyDonHang(maDonHang);
            return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng " + maDonHang));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Autowired
    private com.dede.ticketsystem.service.SessionService sessionService;

    /** API: tạo đơn hàng nhanh (test) */
    @PostMapping("/api/tao-nhanh")
    @ResponseBody
    public ResponseEntity<?> taoDonNhanh(@RequestParam String maSK, @RequestParam int soLuong, @RequestParam(required = false) String maKH) {
        try {
            String currentMaKH = (maKH != null && !maKH.isBlank()) ? maKH : sessionService.getCurrentMaKH();
            if (currentMaKH == null) {
                // Thử lấy khách hàng hiện tại của user, hoặc fallback
                currentMaKH = "KH001"; 
            }
            String currentMaND = sessionService.getCurrentMaND();
            String maNV = currentMaND != null ? currentMaND : "NV001";
            DonHang dh = donHangService.taoDonHang(maSK, soLuong, currentMaKH, maNV);
            return ResponseEntity.ok(Map.of("message", "Tạo đơn hàng thành công", "maDonHang", dh.getMaDonHang()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

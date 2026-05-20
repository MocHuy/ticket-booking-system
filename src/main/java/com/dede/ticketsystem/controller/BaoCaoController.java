package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.BaoCaoSuKienDTO;
import com.dede.ticketsystem.model.BaoCaoTongQuanDTO;
import com.dede.ticketsystem.model.HanhViKhachHangDTO;
import com.dede.ticketsystem.model.SuKien;
import com.dede.ticketsystem.repository.SuKienRepository;
import com.dede.ticketsystem.service.BaoCaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Controller
public class BaoCaoController {

    private final BaoCaoService baoCaoService;
    private final SuKienRepository suKienRepository;

    public BaoCaoController(BaoCaoService baoCaoService, SuKienRepository suKienRepository) {
        this.baoCaoService = baoCaoService;
        this.suKienRepository = suKienRepository;
    }

    @GetMapping("/baocao")
    public String viewDashboard(
            @RequestParam(value = "maSK", required = false) String maSK,
            @RequestParam(value = "tuNgay", required = false) String tuNgay,
            @RequestParam(value = "denNgay", required = false) String denNgay,
            @RequestParam(value = "trangThaiDonHang", required = false) String trangThaiDonHang,
            Model model) {

        Timestamp startTimestamp = null;
        if (tuNgay != null && !tuNgay.trim().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(tuNgay);
                startTimestamp = Timestamp.valueOf(date.atStartOfDay());
            } catch (Exception e) {
                System.err.println("Cảnh báo: Định dạng ngày tuNgay không hợp lệ: " + tuNgay);
            }
        }

        Timestamp endTimestamp = null;
        if (denNgay != null && !denNgay.trim().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(denNgay);
                endTimestamp = Timestamp.valueOf(date.atTime(23, 59, 59, 999000000));
            } catch (Exception e) {
                System.err.println("Cảnh báo: Định dạng ngày denNgay không hợp lệ: " + denNgay);
            }
        }

        // Lấy danh sách sự kiện cho dropdown bộ lọc
        List<SuKien> suKiens = suKienRepository.findAll();
        model.addAttribute("suKiens", suKiens);

        // Lấy báo cáo tổng quan
        BaoCaoTongQuanDTO tongQuan = baoCaoService.getBaoCaoTongQuan(maSK, startTimestamp, endTimestamp, trangThaiDonHang);
        model.addAttribute("tongQuan", tongQuan);

        // Lấy báo cáo chi tiết sự kiện
        List<BaoCaoSuKienDTO> baoCaoSuKienList = baoCaoService.getBaoCaoSuKien(maSK, startTimestamp, endTimestamp, trangThaiDonHang);
        model.addAttribute("baoCaoSuKienList", baoCaoSuKienList);

        // Lấy nhật ký hành vi khách hàng gần nhất
        List<HanhViKhachHangDTO> recentHanhViList = baoCaoService.getRecentHanhViKhachHang(maSK, startTimestamp, endTimestamp);
        model.addAttribute("recentHanhViList", recentHanhViList);

        // Gửi ngược bộ lọc về view
        model.addAttribute("maSKSelected", maSK);
        model.addAttribute("tuNgaySelected", tuNgay);
        model.addAttribute("denNgaySelected", denNgay);
        model.addAttribute("trangThaiSelected", trangThaiDonHang);

        return "BaoCao/dashboard";
    }
}

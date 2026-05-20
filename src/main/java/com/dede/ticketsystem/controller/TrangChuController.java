package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.SuKien;
import com.dede.ticketsystem.model.KhuVuc;
import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.model.DonHang;
import com.dede.ticketsystem.service.SuKienService;
import com.dede.ticketsystem.service.SessionService;
import com.dede.ticketsystem.service.BookingService;
import com.dede.ticketsystem.repository.KhuVucRepository;
import com.dede.ticketsystem.repository.GheRepository;
import com.dede.ticketsystem.repository.DonHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Timestamp;
import java.util.List;

@Controller
public class TrangChuController {

    private final SuKienService suKienService;
    private final KhuVucRepository khuVucRepository;
    private final GheRepository gheRepository;
    private final DonHangRepository donHangRepository;
    private final BookingService bookingService;

    @Autowired
    private SessionService sessionService;

    public TrangChuController(SuKienService suKienService, 
                              KhuVucRepository khuVucRepository, 
                              GheRepository gheRepository,
                              DonHangRepository donHangRepository,
                              BookingService bookingService) {
        this.suKienService = suKienService;
        this.khuVucRepository = khuVucRepository;
        this.gheRepository = gheRepository;
        this.donHangRepository = donHangRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String trangChu(Model model) {
        List<SuKien> list = suKienService.timKiem("", "");
        model.addAttribute("suKienList", list);
        return "Public/index";
    }

    @GetMapping("/su-kien/{maSK}")
    public String chiTietSuKien(@PathVariable String maSK, Model model) {
        SuKien sk = suKienService.timTheoMa(maSK).orElse(null);
        if (sk == null) {
            return "redirect:/";
        }
        
        List<KhuVuc> zones = khuVucRepository.findByMaSK(maSK);
        
        boolean canBuy = false;
        if ("Đang mở bán".equals(sk.getTrangThaiSK())) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            boolean afterMoBan = sk.getThoiGianMoBan() == null || now.after(sk.getThoiGianMoBan());
            boolean beforeDongBan = sk.getThoiGianDongBan() == null || now.before(sk.getThoiGianDongBan());
            if (afterMoBan && beforeDongBan) {
                canBuy = true;
            }
        }
        
        model.addAttribute("suKien", sk);
        model.addAttribute("zones", zones);
        model.addAttribute("canBuy", canBuy);
        return "Public/chi-tiet-su-kien";
    }

    @GetMapping("/mua-ve/{maSK}")
    public String chonGhe(@PathVariable String maSK, Model model) {
        if (!sessionService.isLoggedIn()) {
            return "redirect:/dang-nhap?redirect=/mua-ve/" + maSK;
        }
        if (!sessionService.hasRole("CUSTOMER")) {
            return "redirect:/";
        }
        
        SuKien sk = suKienService.timTheoMa(maSK).orElse(null);
        if (sk == null) {
            return "redirect:/";
        }
        
        List<KhuVuc> zones = khuVucRepository.findByMaSK(maSK);
        List<Ghe> seats = gheRepository.findByMaSK(maSK);
        
        model.addAttribute("suKien", sk);
        model.addAttribute("zones", zones);
        model.addAttribute("seats", seats);
        return "Public/chon-ghe";
    }

    @GetMapping("/thanh-toan")
    public String thanhToan(@RequestParam(required = false) String orderId, Model model) {
        if (!sessionService.isLoggedIn()) {
            return "redirect:/dang-nhap?redirect=/thanh-toan" + (orderId != null ? "?orderId=" + orderId : "");
        }
        
        String maKH = sessionService.getCurrentMaKH();
        if (!sessionService.hasRole("CUSTOMER") || maKH == null) {
            return "redirect:/";
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            return "redirect:/";
        }

        DonHang dh = donHangRepository.findById(orderId).orElse(null);
        if (dh == null) {
            return "redirect:/";
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (!dh.getMaKH().equals(maKH)) {
            return "redirect:/";
        }

        // Kiểm tra trạng thái đơn hàng
        if (!"Chờ thanh toán".equalsIgnoreCase(dh.getTrangThaiDonHang())) {
            return "redirect:/";
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        long remainingSeconds = 0;
        if (dh.getThoiGianHetHan() != null) {
            remainingSeconds = (dh.getThoiGianHetHan().getTime() - now.getTime()) / 1000;
        }

        // Nếu quá hạn thì hủy luôn đơn hàng và giải phóng ghế
        if (remainingSeconds <= 0) {
            try {
                dh.setTrangThaiDonHang("Đã hủy");
                dh.setCapNhatLanCuoi(now);
                donHangRepository.save(dh);
                bookingService.releaseSeats(orderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "redirect:/";
        }

        // Lấy danh sách ghế đang giữ của phiên đơn hàng này
        List<Ghe> gheList = gheRepository.findByMaPhienKhoa(orderId);
        if (gheList.isEmpty()) {
            return "redirect:/";
        }
        
        String maSK = gheList.get(0).getMaSK();
        SuKien sk = suKienService.timTheoMa(maSK).orElse(null);
        
        model.addAttribute("suKien", sk);
        model.addAttribute("gheList", gheList);
        model.addAttribute("tongTien", dh.getThanhTien());
        model.addAttribute("orderId", orderId);
        model.addAttribute("remainingSeconds", remainingSeconds);
        
        return "Public/thanh-toan";
    }
}

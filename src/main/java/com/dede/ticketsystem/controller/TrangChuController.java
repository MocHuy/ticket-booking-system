package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.SuKien;
import com.dede.ticketsystem.model.KhuVuc;
import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.service.SuKienService;
import com.dede.ticketsystem.repository.KhuVucRepository;
import com.dede.ticketsystem.repository.GheRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class TrangChuController {

    private final SuKienService suKienService;
    private final KhuVucRepository khuVucRepository;
    private final GheRepository gheRepository;

    public TrangChuController(SuKienService suKienService, KhuVucRepository khuVucRepository, GheRepository gheRepository) {
        this.suKienService = suKienService;
        this.khuVucRepository = khuVucRepository;
        this.gheRepository = gheRepository;
    }

    @GetMapping("/")
    public String trangChu(Model model) {
        // Chỉ lấy các sự kiện sắp diễn ra hoặc đang mở bán
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
        model.addAttribute("suKien", sk);
        model.addAttribute("zones", zones);
        return "Public/chi-tiet-su-kien";
    }

    @GetMapping("/mua-ve/{maSK}")
    public String chonGhe(@PathVariable String maSK, Model model) {
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
    public String thanhToan(@RequestParam(required = false) String seats, Model model) {
        if (seats == null || seats.trim().isEmpty()) {
            return "redirect:/";
        }
        String[] maGheArray = seats.split(",");
        List<Ghe> gheList = gheRepository.findAllById(List.of(maGheArray));
        if (gheList.isEmpty()) {
            return "redirect:/";
        }
        
        String maSK = gheList.get(0).getMaSK();
        SuKien sk = suKienService.timTheoMa(maSK).orElse(null);
        
        // Calculate total price
        // Note: For real app, price should be fetched from VE or KHUVUC safely
        // In this demo, we can get GiaVe from KHUVUC for each seat
        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        for(Ghe g : gheList) {
            KhuVuc kv = khuVucRepository.findById(g.getMaKhuVuc()).orElse(null);
            if (kv != null && kv.getGiaVe() != null) {
                tongTien = tongTien.add(kv.getGiaVe());
            }
        }
        
        model.addAttribute("suKien", sk);
        model.addAttribute("gheList", gheList);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("seatsParam", seats);
        
        return "Public/thanh-toan";
    }
}

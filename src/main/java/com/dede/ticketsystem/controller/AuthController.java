package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.repository.KhachHangRepository;
import com.dede.ticketsystem.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping("/dang-nhap")
    public String showLogin(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String redirectUrl,
            HttpSession session,
            Model model) {
        if (session.getAttribute("nguoiDung") != null) {
            return "redirect:/";
        }
        String target = (redirect != null && !redirect.isBlank()) ? redirect : redirectUrl;
        model.addAttribute("redirectUrl", target);
        model.addAttribute("showNavbar", false);
        return "auth/login";
    }

    @PostMapping("/dang-nhap")
    public String processLogin(
            @RequestParam String tenTaiKhoan,
            @RequestParam String matKhau,
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String redirectUrl,
            HttpSession session,
            Model model) {
        try {
            NguoiDung nguoiDung = authService.dangNhap(tenTaiKhoan, matKhau);

            // Lưu vào HttpSession
            session.setAttribute("nguoiDung", nguoiDung);
            session.setAttribute("maND", nguoiDung.getMaND());
            session.setAttribute("tenTaiKhoan", nguoiDung.getTenTaiKhoan());

            // Lấy danh sách role của user dạng Set<String>
            Set<String> roles = nguoiDung.getChiTietVaiTros().stream()
                    .map(ct -> ct.getMaVaiTro())
                    .collect(Collectors.toSet());
            session.setAttribute("roles", roles);

            // Nếu user là CUSTOMER thì lấy maKH
            if (roles.contains("CUSTOMER")) {
                khachHangRepository.findByMaND(nguoiDung.getMaND())
                        .ifPresent(kh -> session.setAttribute("maKH", kh.getMaKH()));
            }

            session.setMaxInactiveInterval(30 * 60);

            // Hỗ trợ redirect sau đăng nhập
            String targetRedirect = (redirect != null && !redirect.isBlank()) ? redirect : redirectUrl;
            if (isValidLocalRedirect(targetRedirect)) {
                return "redirect:" + targetRedirect;
            }

            // Redirect mặc định dựa trên role
            if (roles.contains("ADMIN") || roles.contains("ORGANIZER")) {
                return "redirect:/sukien";
            } else if (roles.contains("STAFF")) {
                return "redirect:/soat-ve";
            } else {
                return "redirect:/";
            }

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tenTaiKhoanCu", tenTaiKhoan);
            String targetRedirect = (redirect != null && !redirect.isBlank()) ? redirect : redirectUrl;
            model.addAttribute("redirectUrl", targetRedirect);
            model.addAttribute("showNavbar", false);
            return "auth/login";
        }
    }

    @GetMapping("/dang-ky")
    public String showRegister(HttpSession session, Model model) {
        if (session.getAttribute("nguoiDung") != null) {
            return "redirect:/";
        }
        model.addAttribute("showNavbar", false);
        return "auth/register";
    }

    @PostMapping("/dang-ky")
    public String processRegister(
            @RequestParam String tenTaiKhoan,
            @RequestParam String email,
            @RequestParam String sdt,
            @RequestParam String matKhau,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            authService.dangKy(tenTaiKhoan, email, sdt, matKhau);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/dang-nhap";
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.contains("JDBC exception") || errorMessage.contains("SQL")) {
                errorMessage = "Lỗi hệ thống khi đăng ký. Vui lòng thử lại sau.";
            }
            model.addAttribute("error", errorMessage);
            model.addAttribute("tenTaiKhoan", tenTaiKhoan);
            model.addAttribute("email", email);
            model.addAttribute("sdt", sdt);
            model.addAttribute("showNavbar", false);
            return "auth/register";
        }
    }

    @GetMapping("/dang-xuat")
    public String logoutGet(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công.");
        return "redirect:/";
    }

    @PostMapping("/dang-xuat")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công.");
        return "redirect:/dang-nhap";
    }

    private boolean isValidLocalRedirect(String path) {
        return path != null && path.startsWith("/") && !path.startsWith("//");
    }
}

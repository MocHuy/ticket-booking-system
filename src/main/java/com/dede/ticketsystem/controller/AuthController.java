package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/dang-nhap")
    public String showLogin(
            @RequestParam(required = false) String redirectUrl,
            HttpSession session,
            Model model
    ) {
        if (session.getAttribute("nguoiDung") != null) {
            return "redirect:/";
        }
        model.addAttribute("redirectUrl", redirectUrl);
        model.addAttribute("showNavbar", false);
        return "auth/login";
    }

    @PostMapping("/dang-nhap")
    public String processLogin(
            @RequestParam String tenTaiKhoan,
            @RequestParam String matKhau,
            @RequestParam(required = false) String redirectUrl,
            HttpSession session,
            Model model
    ) {
        try {
            NguoiDung nguoiDung = authService.dangNhap(tenTaiKhoan, matKhau);

            session.setAttribute("nguoiDung", nguoiDung);
            session.setMaxInactiveInterval(30 * 60);

            if (redirectUrl != null && !redirectUrl.isBlank()) {
                return "redirect:" + redirectUrl;
            }
            
            return nguoiDung.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/su-kien";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tenTaiKhoanCu", tenTaiKhoan);
            model.addAttribute("redirectUrl", redirectUrl);
            model.addAttribute("showNavbar", false);
            return "auth/login";
        }
    }

    @PostMapping("/dang-xuat")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công.");
        return "redirect:/dang-nhap";
    }
}

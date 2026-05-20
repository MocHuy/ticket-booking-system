package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.NguoiDung;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class SessionService {

    @Autowired
    private HttpServletRequest request;

    private HttpSession getSession() {
        return request.getSession(false);
    }

    public boolean isLoggedIn() {
        HttpSession session = getSession();
        return session != null && session.getAttribute("nguoiDung") != null;
    }

    public NguoiDung getCurrentUser() {
        HttpSession session = getSession();
        if (session == null) {
            return null;
        }
        return (NguoiDung) session.getAttribute("nguoiDung");
    }

    public String getCurrentMaND() {
        HttpSession session = getSession();
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("maND");
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public String getCurrentMaNV() {
        String currentMaND = getCurrentMaND();
        if (currentMaND == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("SELECT MaNV FROM NHANVIEN WHERE MaND = ?", String.class, currentMaND);
        } catch (Exception e) {
            try {
                Set<String> roles = getCurrentRoles();
                if (roles.contains("ADMIN") || roles.contains("STAFF") || roles.contains("ORGANIZER")) {
                    String username = (String) request.getSession().getAttribute("tenTaiKhoan");
                    if (username == null) {
                        username = currentMaND;
                    }
                    String maNV = "NV-" + username.toUpperCase();
                    
                    Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM NHANVIEN WHERE MaNV = ?", Integer.class, maNV);
                    if (exists != null && exists > 0) {
                        maNV = "NV-" + currentMaND.toUpperCase();
                    }
                    
                    String loaiNV = "Quản lý";
                    double luongCB = 15000000;
                    double phuCap = 2000000;
                    if (roles.contains("STAFF")) {
                        loaiNV = "Nhân viên soát vé";
                        luongCB = 8000000;
                        phuCap = 500000;
                    } else if (roles.contains("ORGANIZER")) {
                        loaiNV = "Ban tổ chức";
                        luongCB = 12000000;
                        phuCap = 1000000;
                    }
                    
                    jdbcTemplate.update("INSERT INTO NHANVIEN (MaNV, LoaiNV, NgayVaoLam, TrangThaiLamViec, LuongCoBan, PhuCap, MaND) VALUES (?, ?, SYSTIMESTAMP, 'Đang làm việc', ?, ?, ?)",
                            maNV, loaiNV, luongCB, phuCap, currentMaND);
                    return maNV;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    public String getCurrentMaKH() {
        HttpSession session = getSession();
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("maKH");
    }

    @SuppressWarnings("unchecked")
    public Set<String> getCurrentRoles() {
        HttpSession session = getSession();
        if (session == null) {
            return Collections.emptySet();
        }
        Set<String> roles = (Set<String>) session.getAttribute("roles");
        return roles != null ? roles : Collections.emptySet();
    }

    public boolean hasRole(String role) {
        return getCurrentRoles().contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        Set<String> currentRoles = getCurrentRoles();
        for (String r : roles) {
            if (currentRoles.contains(r)) {
                return true;
            }
        }
        return false;
    }
}

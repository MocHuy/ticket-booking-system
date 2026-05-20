package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.model.KhachHang;
import com.dede.ticketsystem.model.VaiTro;
import com.dede.ticketsystem.model.ChiTietVaiTro;
import com.dede.ticketsystem.repository.NguoiDungRepository;
import com.dede.ticketsystem.repository.KhachHangRepository;
import com.dede.ticketsystem.repository.VaiTroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

@Service
public class AuthService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public NguoiDung dangNhap(String tenTaiKhoan, String matKhau) {
        NguoiDung nguoiDung = nguoiDungRepository
                .findByTenTaiKhoan(tenTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại."));

        if ("Bị khóa".equals(nguoiDung.getTrangThaiND())) {
            throw new RuntimeException("Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        if (!passwordEncoder.matches(matKhau, nguoiDung.getMatKhauMaHoa())) {
            throw new RuntimeException("Mật khẩu không chính xác.");
        }

        // Cập nhật LanCuoiDangNhap = now
        nguoiDung.setLanCuoiDangNhap(new Timestamp(System.currentTimeMillis()));
        return nguoiDungRepository.save(nguoiDung);
    }

    @Transactional
    public void dangKy(String tenTaiKhoan, String email, String sdt, String matKhau) {
        // Validate username/email không trùng
        if (nguoiDungRepository.findByTenTaiKhoan(tenTaiKhoan).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại.");
        }
        if (email != null && !email.isBlank() && nguoiDungRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng.");
        }
        
        // 1. Tạo NGUOIDUNG
        String newMaND = "ND" + System.currentTimeMillis();
        NguoiDung newUser = NguoiDung.builder()
                .maND(newMaND)
                .tenTaiKhoan(tenTaiKhoan)
                .email(email)
                .sdt(sdt)
                .matKhauMaHoa(passwordEncoder.encode(matKhau))
                .trangThaiND("Đang hoạt động")
                .thoiGianTao(new Timestamp(System.currentTimeMillis()))
                .chiTietVaiTros(new ArrayList<>())
                .build();
        
        // 2. Gán role CUSTOMER
        // Đảm bảo role CUSTOMER đã tồn tại
        VaiTro customerRole = vaiTroRepository.findById("CUSTOMER")
                .orElseGet(() -> {
                    VaiTro r = VaiTro.builder().maVaiTro("CUSTOMER").tenVaiTro("CUSTOMER").moTa("Khách hàng").build();
                    return vaiTroRepository.save(r);
                });
                
        ChiTietVaiTro chiTiet = new ChiTietVaiTro();
        chiTiet.setNguoiDung(newUser);
        chiTiet.setVaiTro(customerRole);
        chiTiet.setMaND(newUser.getMaND());
        chiTiet.setMaVaiTro("CUSTOMER");
        
        newUser.getChiTietVaiTros().add(chiTiet);
        
        // Lưu NGUOIDUNG
        NguoiDung savedUser = nguoiDungRepository.save(newUser);
        
        // 3. Tạo KHACHHANG tương ứng
        String newMaKH = "KH" + (System.currentTimeMillis() % 100000000);
        KhachHang khachHang = new KhachHang();
        khachHang.setMaKH(newMaKH);
        khachHang.setHoTenKH(tenTaiKhoan); // HoTenKH = TenTaiKhoan nếu chưa có họ tên riêng
        khachHang.setTongChiTieu(BigDecimal.ZERO);
        khachHang.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));
        khachHang.setNguoiDung(savedUser);
        
        khachHangRepository.save(khachHang);
    }
}

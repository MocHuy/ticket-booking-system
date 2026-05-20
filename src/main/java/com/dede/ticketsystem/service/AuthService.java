package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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

        return nguoiDung;
    }

    public void dangKy(String tenTaiKhoan, String email, String sdt, String matKhau) {
        if (nguoiDungRepository.findByTenTaiKhoan(tenTaiKhoan).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại.");
        }
        
        NguoiDung newUser = NguoiDung.builder()
                .maND("ND" + System.currentTimeMillis())
                .tenTaiKhoan(tenTaiKhoan)
                .email(email)
                .sdt(sdt)
                .matKhauMaHoa(passwordEncoder.encode(matKhau))
                .trangThaiND("Đang hoạt động")
                .thoiGianTao(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();
        
        nguoiDungRepository.save(newUser);
    }
}

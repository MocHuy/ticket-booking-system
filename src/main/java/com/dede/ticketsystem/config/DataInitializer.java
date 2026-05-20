package com.dede.ticketsystem.config;

import com.dede.ticketsystem.model.ChiTietVaiTro;
import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.model.VaiTro;
import com.dede.ticketsystem.model.KhachHang;
import com.dede.ticketsystem.repository.NguoiDungRepository;
import com.dede.ticketsystem.repository.VaiTroRepository;
import com.dede.ticketsystem.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Tạo các vai trò mặc định
        List<VaiTro> defaultRoles = Arrays.asList(
            VaiTro.builder().maVaiTro("ADMIN").tenVaiTro("ADMIN").moTa("Quản trị viên hệ thống").build(),
            VaiTro.builder().maVaiTro("CUSTOMER").tenVaiTro("CUSTOMER").moTa("Khách hàng").build(),
            VaiTro.builder().maVaiTro("STAFF").tenVaiTro("STAFF").moTa("Nhân viên").build(),
            VaiTro.builder().maVaiTro("ORGANIZER").tenVaiTro("ORGANIZER").moTa("Ban tổ chức").build()
        );

        for (VaiTro r : defaultRoles) {
            if (!vaiTroRepository.existsById(r.getMaVaiTro())) {
                vaiTroRepository.save(r);
            }
        }

        // 2. Khởi tạo các user demo
        initUser("admin", "123456", "ADMIN", "ND-ADMIN");
        initUser("customer", "123456", "CUSTOMER", "ND-CUSTOMER");
        initUser("staff", "123456", "STAFF", "ND-STAFF");
        initUser("organizer", "123456", "ORGANIZER", "ND-ORGANIZER");

        System.out.println("KHỞI TẠO DỮ LIỆU BAN ĐẦU HOÀN TẤT VÀ IDEMPOTENT.");
    }

    private void initUser(String username, String rawPassword, String roleCode, String maND) {
        Optional<NguoiDung> existing = nguoiDungRepository.findByTenTaiKhoan(username);
        NguoiDung user;
        
        if (existing.isEmpty()) {
            VaiTro role = vaiTroRepository.findById(roleCode).orElse(null);
            if (role == null) return;

            user = NguoiDung.builder()
                    .maND(maND)
                    .tenTaiKhoan(username)
                    .matKhauMaHoa(passwordEncoder.encode(rawPassword))
                    .trangThaiND("Đang hoạt động")
                    .thoiGianTao(new Timestamp(System.currentTimeMillis()))
                    .chiTietVaiTros(new ArrayList<>())
                    .email(username + "@ticketsystem.com")
                    .sdt("090" + String.format("%07d", (int)(Math.random() * 10000000)))
                    .build();

            ChiTietVaiTro chiTiet = new ChiTietVaiTro();
            chiTiet.setNguoiDung(user);
            chiTiet.setVaiTro(role);
            chiTiet.setMaND(user.getMaND());
            chiTiet.setMaVaiTro(roleCode);

            user.getChiTietVaiTros().add(chiTiet);
            user = nguoiDungRepository.save(user);
            System.out.println("Tạo thành công user demo: " + username);
        } else {
            user = existing.get();
        }

        // 3. Khởi tạo KHACHHANG hoặc NHANVIEN tương ứng
        if ("CUSTOMER".equals(roleCode)) {
            // Đảm bảo có bản ghi KHACHHANG
            Optional<KhachHang> khOpt = khachHangRepository.findByMaND(user.getMaND());
            if (khOpt.isEmpty()) {
                KhachHang kh = new KhachHang();
                kh.setMaKH("KH-" + user.getTenTaiKhoan().toUpperCase());
                kh.setHoTenKH("Khách hàng " + user.getTenTaiKhoan());
                kh.setTongChiTieu(BigDecimal.ZERO);
                kh.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));
                kh.setNguoiDung(user);
                kh.setMaHangThanhVien(null); // để null để tránh foreign key constraint nếu chưa có dữ liệu HANGTHANHVIEN

                khachHangRepository.save(kh);
                System.out.println("Khởi tạo KHACHHANG cho user customer thành công");
            }
        } else {
            // Đảm bảo có bản ghi NHANVIEN
            String checkSql = "SELECT COUNT(*) FROM NHANVIEN WHERE MaND = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, user.getMaND());
            if (count == null || count == 0) {
                String loaiNV = "Quản lý";
                double luongCB = 15000000;
                double phuCap = 2000000;

                if ("STAFF".equals(roleCode)) {
                    loaiNV = "Nhân viên soát vé";
                    luongCB = 8000000;
                    phuCap = 500000;
                } else if ("ORGANIZER".equals(roleCode)) {
                    loaiNV = "Ban tổ chức";
                    luongCB = 12000000;
                    phuCap = 1000000;
                }

                String insertSql = "INSERT INTO NHANVIEN (MaNV, LoaiNV, NgayVaoLam, TrangThaiLamViec, LuongCoBan, PhuCap, MaND) " +
                                   "VALUES (?, ?, SYSTIMESTAMP, 'Đang làm việc', ?, ?, ?)";
                jdbcTemplate.update(insertSql, "NV-" + user.getTenTaiKhoan().toUpperCase(), loaiNV, luongCB, phuCap, user.getMaND());
                System.out.println("Khởi tạo NHANVIEN cho user " + username + " thành công");
            }
        }
    }
}
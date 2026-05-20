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

        initTestData();

        System.out.println("KHỞI TẠO DỮ LIỆU BAN ĐẦU HOÀN TẤT VÀ IDEMPOTENT.");
    }

    private void initTestData() {
        System.out.println("Bắt đầu khởi tạo dữ liệu mẫu cho Vé và Sự kiện...");
        try {
            // 1. DIADIEM
            Integer countDiaDiem = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DIADIEM WHERE MaDiaDiem = 'DD001'", Integer.class);
            if (countDiaDiem == null || countDiaDiem == 0) {
                jdbcTemplate.update("INSERT INTO DIADIEM (MaDiaDiem, TenDiaDiem, DiaChi, ThanhPho, SucChuaToiDa, MoTa, TrangThai) VALUES ('DD001', 'Nhà thi đấu Phú Thọ', '01 Lữ Gia, Phường 15, Quận 11', 'TP.HCM', 8000, 'Nhà thi đấu đa năng', 'Đang hoạt động')");
                System.out.println("- Đã khởi tạo DIADIEM: DD001");
            }

            // 2. LOAISUKIEN
            Integer countLoaiSK1 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOAISUKIEN WHERE MaLoaiSK = 'LSK001'", Integer.class);
            if (countLoaiSK1 == null || countLoaiSK1 == 0) {
                jdbcTemplate.update("INSERT INTO LOAISUKIEN (MaLoaiSK, TenLoaiSK, MoTa) VALUES ('LSK001', 'Concert', 'Sự kiện âm nhạc trực tiếp')");
                System.out.println("- Đã khởi tạo LOAISUKIEN: Concert");
            }
            Integer countLoaiSK2 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOAISUKIEN WHERE MaLoaiSK = 'LSK002'", Integer.class);
            if (countLoaiSK2 == null || countLoaiSK2 == 0) {
                jdbcTemplate.update("INSERT INTO LOAISUKIEN (MaLoaiSK, TenLoaiSK, MoTa) VALUES ('LSK002', 'Workshop', 'Buổi thực hành chia sẻ kỹ năng')");
                System.out.println("- Đã khởi tạo LOAISUKIEN: Workshop");
            }
            Integer countLoaiSK3 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOAISUKIEN WHERE MaLoaiSK = 'LSK003'", Integer.class);
            if (countLoaiSK3 == null || countLoaiSK3 == 0) {
                jdbcTemplate.update("INSERT INTO LOAISUKIEN (MaLoaiSK, TenLoaiSK, MoTa) VALUES ('LSK003', 'Hội thảo', 'Buổi thảo luận học thuật chuyên đề')");
                System.out.println("- Đã khởi tạo LOAISUKIEN: Hội thảo");
            }

            // 3. SUKIEN
            Integer countSK = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SUKIEN WHERE MaSK = 'SK001'", Integer.class);
            if (countSK == null || countSK == 0) {
                jdbcTemplate.update("INSERT INTO SUKIEN (MaSK, TenSK, MoTa, ThoiGianBatDau, ThoiGianKetThuc, ThoiGianMoBan, ThoiGianDongBan, TrangThaiSK, MaLoaiSK, MaDiaDiem, TongSoVe, SoVeDaBan, ThoiGianTao, CapNhatLanCuoi) " +
                        "VALUES ('SK001', 'Dề Dê Summer Concert 2026', 'Đêm nhạc hoành tráng mùa hè 2026', TO_TIMESTAMP('2026-07-15 19:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-07-15 23:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-05-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-07-14 23:59:59', 'YYYY-MM-DD HH24:MI:SS'), 'Đang mở bán', 'LSK001', 'DD001', 5000, 2, SYSTIMESTAMP, SYSTIMESTAMP)");
                System.out.println("- Đã khởi tạo SUKIEN: SK001");
            } else {
                jdbcTemplate.update("UPDATE SUKIEN SET TrangThaiSK = 'Đang mở bán' WHERE MaSK = 'SK001'");
            }

            // 4. KHUVUC
            Integer countKVStandard = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KHUVUC WHERE MaKhuVuc = 'KV001_A'", Integer.class);
            if (countKVStandard == null || countKVStandard == 0) {
                jdbcTemplate.update("INSERT INTO KHUVUC (MaKhuVuc, TenKhuVuc, MauSacHienThi, SoGheToiDa, SoGheDaBan, SoVeToiDaPerKH, GiaVe, TrangThai, MaSK) VALUES ('KV001_A', 'Khu Standard', '#FF6B6B', 50, 1, 4, 300000, 'Đang bán', 'SK001')");
            } else {
                jdbcTemplate.update("UPDATE KHUVUC SET TenKhuVuc = 'Khu Standard', GiaVe = 300000, SoVeToiDaPerKH = 4 WHERE MaKhuVuc = 'KV001_A'");
            }
            
            Integer countKVVIP = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KHUVUC WHERE MaKhuVuc = 'KV001_VIP'", Integer.class);
            if (countKVVIP == null || countKVVIP == 0) {
                jdbcTemplate.update("INSERT INTO KHUVUC (MaKhuVuc, TenKhuVuc, MauSacHienThi, SoGheToiDa, SoGheDaBan, SoVeToiDaPerKH, GiaVe, TrangThai, MaSK) VALUES ('KV001_VIP', 'Khu VIP', '#FFD700', 15, 1, 2, 1000000, 'Đang bán', 'SK001')");
            } else {
                jdbcTemplate.update("UPDATE KHUVUC SET TenKhuVuc = 'Khu VIP', GiaVe = 1000000, SoVeToiDaPerKH = 2 WHERE MaKhuVuc = 'KV001_VIP'");
            }
            System.out.println("- Đã khởi tạo KHUVUC cho SK001");

            // 5. GHENGOI
            // VIP: 3 rows x 5 columns = 15 seats (HangGhe: V, W, X; CotGhe: 1..5)
            String[] vipRows = {"V", "W", "X"};
            for (String r : vipRows) {
                for (int c = 1; c <= 5; c++) {
                    String maGhe = "KV001_VIP_" + r + String.format("%02d", c);
                    String tenGhe = r + String.format("%02d", c);
                    
                    Integer countGhe = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM GHENGOI WHERE MaGhe = ?", Integer.class, maGhe);
                    if (countGhe == null || countGhe == 0) {
                        String defaultStatus = ("V".equals(r) && c == 1) ? "Đã bán" : "Trống";
                        jdbcTemplate.update("INSERT INTO GHENGOI (MaGhe, TenGhe, HangGhe, CotGhe, TrangThaiGhe, MaKhuVuc, MaSK) VALUES (?, ?, ?, ?, ?, 'KV001_VIP', 'SK001')",
                                maGhe, tenGhe, r, c, defaultStatus);
                    }
                }
            }

            // Standard: 5 rows x 10 columns = 50 seats (HangGhe: A, B, C, D, E; CotGhe: 1..10)
            String[] stdRows = {"A", "B", "C", "D", "E"};
            for (String r : stdRows) {
                for (int c = 1; c <= 10; c++) {
                    String maGhe = "KV001_A_" + r + String.format("%02d", c);
                    String tenGhe = r + String.format("%02d", c);
                    
                    Integer countGhe = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM GHENGOI WHERE MaGhe = ?", Integer.class, maGhe);
                    if (countGhe == null || countGhe == 0) {
                        String defaultStatus = ("A".equals(r) && c == 1) ? "Đã bán" : "Trống";
                        jdbcTemplate.update("INSERT INTO GHENGOI (MaGhe, TenGhe, HangGhe, CotGhe, TrangThaiGhe, MaKhuVuc, MaSK) VALUES (?, ?, ?, ?, ?, 'KV001_A', 'SK001')",
                                maGhe, tenGhe, r, c, defaultStatus);
                    }
                }
            }
            System.out.println("- Đã khởi tạo GHENGOI");

            // 6. DONHANG
            Integer countDH = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DONHANG WHERE MaDonHang = 'DH_20260501_001'", Integer.class);
            if (countDH == null || countDH == 0) {
                jdbcTemplate.update("INSERT INTO DONHANG (MaDonHang, SoDonHang, TongTien, ThanhTien, TrangThaiDonHang, ThoiGianDat, ThoiGianHetHan, MaKH) VALUES ('DH_20260501_001', 'SO-2026-001', 300000, 300000, 'Đã thanh toán', TO_TIMESTAMP('2026-05-01 10:05:00','YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-05-01 10:20:00','YYYY-MM-DD HH24:MI:SS'), 'KH-CUSTOMER')");
                jdbcTemplate.update("INSERT INTO DONHANG (MaDonHang, SoDonHang, TongTien, ThanhTien, TrangThaiDonHang, ThoiGianDat, ThoiGianHetHan, MaKH) VALUES ('DH_20260501_002', 'SO-2026-002', 1000000, 1000000, 'Đã thanh toán', TO_TIMESTAMP('2026-05-01 10:10:00','YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-05-01 10:25:00','YYYY-MM-DD HH24:MI:SS'), 'KH-CUSTOMER')");
                System.out.println("- Đã khởi tạo DONHANG");
            } else {
                jdbcTemplate.update("UPDATE DONHANG SET TongTien = 300000, ThanhTien = 300000 WHERE MaDonHang = 'DH_20260501_001'");
                jdbcTemplate.update("UPDATE DONHANG SET TongTien = 1000000, ThanhTien = 1000000 WHERE MaDonHang = 'DH_20260501_002'");
            }

            // 7. VE
            Integer countVe = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM VE WHERE MaVe = 'VE_SK001_A01_001'", Integer.class);
            if (countVe == null || countVe == 0) {
                jdbcTemplate.update("INSERT INTO VE (MaVe, MaQR, GiaVe, TrangThaiVe, ThoiGianPhat, MaDonHang, MaGhe, MaSK) VALUES ('VE_SK001_A01_001', 'QR_HASH_SHA256_A01_ABCDEF1234567890', 300000, 'Chưa sử dụng', TO_TIMESTAMP('2026-05-01 10:06:00','YYYY-MM-DD HH24:MI:SS'), 'DH_20260501_001', 'KV001_A_A01', 'SK001')");
                jdbcTemplate.update("INSERT INTO VE (MaVe, MaQR, GiaVe, TrangThaiVe, ThoiGianPhat, MaDonHang, MaGhe, MaSK) VALUES ('VE_SK001_VIP01_001', 'QR_HASH_SHA256_VIP01_FEDCBA9876543210', 1000000, 'Chưa sử dụng', TO_TIMESTAMP('2026-05-01 10:11:00','YYYY-MM-DD HH24:MI:SS'), 'DH_20260501_002', 'KV001_VIP_V01', 'SK001')");
                System.out.println("- Đã khởi tạo VE test");
            } else {
                jdbcTemplate.update("UPDATE VE SET GiaVe = 300000 WHERE MaVe = 'VE_SK001_A01_001'");
                jdbcTemplate.update("UPDATE VE SET GiaVe = 1000000 WHERE MaVe = 'VE_SK001_VIP01_001'");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi tạo dữ liệu mẫu: " + e.getMessage());
            e.printStackTrace();
        }
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
            user = nguoiDungRepository.saveAndFlush(user);
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
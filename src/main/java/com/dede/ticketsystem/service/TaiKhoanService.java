package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.*;
import com.dede.ticketsystem.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaiKhoanService {

    private final NguoiDungRepository nguoiDungRepo;
    private final VaiTroRepository vaiTroRepo;
    private final ChiTietVaiTroRepository chiTietVaiTroRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    // Tự code Constructor thủ công thay cho @RequiredArgsConstructor của Lombok
    public TaiKhoanService(NguoiDungRepository nguoiDungRepo, 
                           VaiTroRepository vaiTroRepo, 
                           ChiTietVaiTroRepository chiTietVaiTroRepo, 
                           BCryptPasswordEncoder passwordEncoder) {
        this.nguoiDungRepo = nguoiDungRepo;
        this.vaiTroRepo = vaiTroRepo;
        this.chiTietVaiTroRepo = chiTietVaiTroRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<NguoiDung> getDanhSachTatCa() {
        return nguoiDungRepo.findAll();
    }

    public List<NguoiDung> timKiem(String keyword) {
        if (keyword == null || keyword.isBlank()) return getDanhSachTatCa();
        return nguoiDungRepo.timKiemTheoTenHoacEmail(keyword.trim());
    }

    public List<NguoiDung> locTheoTrangThai(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) return getDanhSachTatCa();
        return nguoiDungRepo.findByTrangThaiND(trangThai);
    }

    public List<VaiTro> getDanhSachVaiTro() {
        return vaiTroRepo.findAll();
    }

    public Optional<NguoiDung> timTheoMa(String maND) {
        return nguoiDungRepo.findById(maND);
    }

    @Transactional
    public NguoiDung taoTaiKhoan(TaiKhoanDTO dto) {
        validateTaoMoi(dto);

        String maND = "ND" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        NguoiDung nd = NguoiDung.builder()
                .maND(maND)
                .tenTaiKhoan(dto.getTenTaiKhoan().trim())
                .matKhauMaHoa(passwordEncoder.encode(dto.getMatKhau()))
                .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                .sdt(dto.getSdt())
                .gioiTinh(dto.getGioiTinh())
                .ngaySinh(dto.getNgaySinh() != null ? new Date(dto.getNgaySinh().getTime()) : null)
                .trangThaiND(dto.getTrangThaiND() != null ? dto.getTrangThaiND() : "Đang hoạt động")
                .thoiGianTao(Timestamp.valueOf(LocalDateTime.now()))
                .capNhatLanCuoi(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        nguoiDungRepo.save(nd);

        if (dto.getDanhSachVaiTro() != null && !dto.getDanhSachVaiTro().isEmpty()) {
            ganVaiTro(maND, dto.getDanhSachVaiTro());
        }

        return nd;
    }


    @Transactional
    public NguoiDung capNhatTaiKhoan(String maND, TaiKhoanDTO dto) {
        NguoiDung nd = nguoiDungRepo.findById(maND)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + maND));

        if (!nd.getTenTaiKhoan().equals(dto.getTenTaiKhoan()) &&
            nguoiDungRepo.existsByTenTaiKhoan(dto.getTenTaiKhoan())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại!");
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(nd.getEmail()) &&
            nguoiDungRepo.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        nd.setTenTaiKhoan(dto.getTenTaiKhoan().trim());
        nd.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        nd.setSdt(dto.getSdt());
        nd.setGioiTinh(dto.getGioiTinh());
        nd.setNgaySinh(dto.getNgaySinh() != null ? new Date(dto.getNgaySinh().getTime()) : null);
        nd.setTrangThaiND(dto.getTrangThaiND());
        nd.setCapNhatLanCuoi(Timestamp.valueOf(LocalDateTime.now()));

        if (dto.getMatKhau() != null && !dto.getMatKhau().isBlank()) {
            nd.setMatKhauMaHoa(passwordEncoder.encode(dto.getMatKhau()));
        }

        nguoiDungRepo.save(nd);

        chiTietVaiTroRepo.deleteByMaND(maND);
        if (dto.getDanhSachVaiTro() != null && !dto.getDanhSachVaiTro().isEmpty()) {
            ganVaiTro(maND, dto.getDanhSachVaiTro());
        }

        return nd;
    }

    @Transactional
    public void xoaTaiKhoan(String maND) {
        NguoiDung nd = nguoiDungRepo.findById(maND)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + maND));
        nd.setTrangThaiND("Bị khóa");
        nd.setCapNhatLanCuoi(Timestamp.valueOf(LocalDateTime.now()));
        nguoiDungRepo.save(nd);
    }


    private void ganVaiTro(String maND, List<String> danhSachMaVaiTro) {
        List<ChiTietVaiTro> chiTiets = danhSachMaVaiTro.stream()
                .map(maVaiTro -> ChiTietVaiTro.builder()
                        .maND(maND)
                        .maVaiTro(maVaiTro)
                        .build())
                .collect(Collectors.toList());
        chiTietVaiTroRepo.saveAll(chiTiets);
    }

    private void validateTaoMoi(TaiKhoanDTO dto) {
        if (dto.getTenTaiKhoan() == null || dto.getTenTaiKhoan().isBlank()) {
            throw new RuntimeException("Tên tài khoản không được để trống!");
        }
        if (nguoiDungRepo.existsByTenTaiKhoan(dto.getTenTaiKhoan().trim())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại!");
        }
        if (dto.getMatKhau() == null || dto.getMatKhau().isBlank()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank() &&
            nguoiDungRepo.existsByEmail(dto.getEmail().trim())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
    }
}
package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.SuKien;
import com.dede.ticketsystem.model.SuKienDTO;
import com.dede.ticketsystem.model.ThietLapSanKhauDTO;
import com.dede.ticketsystem.model.Ve;
import com.dede.ticketsystem.model.KhuVuc;
import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.repository.SuKienRepository;
import com.dede.ticketsystem.repository.VeRepository;
import com.dede.ticketsystem.repository.KhuVucRepository;
import com.dede.ticketsystem.repository.GheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class SuKienService {

    private final SuKienRepository suKienRepository;
    private final VeRepository veRepository;
    private final KhuVucRepository khuVucRepository;
    private final GheRepository gheRepository;

    public SuKienService(SuKienRepository suKienRepository, VeRepository veRepository, KhuVucRepository khuVucRepository, GheRepository gheRepository) {
        this.suKienRepository = suKienRepository;
        this.veRepository = veRepository;
        this.khuVucRepository = khuVucRepository;
        this.gheRepository = gheRepository;
    }

    public List<SuKien> layTatCa() {
        return suKienRepository.findAll();
    }

    public List<SuKien> timKiem(String keyword, String trangThai) {
        return suKienRepository.search(keyword, trangThai);
    }

    public Optional<SuKien> timTheoMa(String maSK) {
        return suKienRepository.findById(maSK);
    }

    private Timestamp parseTimestamp(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            return Timestamp.valueOf(timeStr.replace("T", " ") + ":00");
        } catch (Exception e) {
            return null;
        }
    }

    public SuKien taoSuKien(SuKienDTO dto) {
        SuKien sk = new SuKien();

        if (dto.getMaSK() == null || dto.getMaSK().trim().isEmpty()) {
            sk.setMaSK("SK-" + System.currentTimeMillis());
        } else {
            if (suKienRepository.existsById(dto.getMaSK())) {
                throw new RuntimeException("Mã sự kiện đã tồn tại!");
            }
            sk.setMaSK(dto.getMaSK());
        }

        sk.setTenSK(dto.getTenSK());
        sk.setMoTa(dto.getMoTa());
        sk.setHinhAnh(dto.getHinhAnh());
        sk.setHinhAnhThumb(dto.getHinhAnhThumb());
        sk.setMoTaNgan(dto.getMoTaNgan());
        sk.setTags(dto.getTags());

        sk.setThoiGianBatDau(parseTimestamp(dto.getThoiGianBatDau()));
        sk.setThoiGianKetThuc(parseTimestamp(dto.getThoiGianKetThuc()));
        sk.setThoiGianMoBan(parseTimestamp(dto.getThoiGianMoBan()));
        sk.setThoiGianDongBan(parseTimestamp(dto.getThoiGianDongBan()));

        sk.setTongSoVe(dto.getTongSoVe() == null ? 0 : dto.getTongSoVe());
        sk.setSoVeDaBan(0);
        sk.setTrangThaiSK(dto.getTrangThaiSK() == null ? "Chưa mở bán" : dto.getTrangThaiSK());
        sk.setThoiGianTao(new Timestamp(System.currentTimeMillis()));

        sk.setMaLoaiSK(dto.getMaLoaiSK() != null && dto.getMaLoaiSK().isBlank() ? null : dto.getMaLoaiSK());
        sk.setMaDiaDiem(dto.getMaDiaDiem() != null && dto.getMaDiaDiem().isBlank() ? null : dto.getMaDiaDiem());
        sk.setMaNV(dto.getMaNV() != null && dto.getMaNV().isBlank() ? null : dto.getMaNV());

        SuKien savedSk = suKienRepository.save(sk);

        // We no longer auto generate tickets here. It is moved to thietLapSanKhau.

        return savedSk;
    }

    public SuKien capNhatSuKien(String maSK, SuKienDTO dto) {
        SuKien sk = suKienRepository.findById(maSK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + maSK));

        if (dto.getTenSK() != null) sk.setTenSK(dto.getTenSK());
        if (dto.getMoTa() != null) sk.setMoTa(dto.getMoTa());
        if (dto.getHinhAnh() != null) sk.setHinhAnh(dto.getHinhAnh());
        if (dto.getHinhAnhThumb() != null) sk.setHinhAnhThumb(dto.getHinhAnhThumb());
        if (dto.getMoTaNgan() != null) sk.setMoTaNgan(dto.getMoTaNgan());
        if (dto.getTags() != null) sk.setTags(dto.getTags());

        if (dto.getThoiGianBatDau() != null) sk.setThoiGianBatDau(parseTimestamp(dto.getThoiGianBatDau()));
        if (dto.getThoiGianKetThuc() != null) sk.setThoiGianKetThuc(parseTimestamp(dto.getThoiGianKetThuc()));
        if (dto.getThoiGianMoBan() != null) sk.setThoiGianMoBan(parseTimestamp(dto.getThoiGianMoBan()));
        if (dto.getThoiGianDongBan() != null) sk.setThoiGianDongBan(parseTimestamp(dto.getThoiGianDongBan()));

        if (dto.getTongSoVe() != null) sk.setTongSoVe(dto.getTongSoVe());
        if (dto.getTrangThaiSK() != null) sk.setTrangThaiSK(dto.getTrangThaiSK());
        
        sk.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));

        if (dto.getMaLoaiSK() != null) sk.setMaLoaiSK(dto.getMaLoaiSK().isBlank() ? null : dto.getMaLoaiSK());
        if (dto.getMaDiaDiem() != null) sk.setMaDiaDiem(dto.getMaDiaDiem().isBlank() ? null : dto.getMaDiaDiem());
        if (dto.getMaNV() != null) sk.setMaNV(dto.getMaNV().isBlank() ? null : dto.getMaNV());

        return suKienRepository.save(sk);
    }

    public void huySuKien(String maSK) {
        SuKien sk = suKienRepository.findById(maSK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + maSK));
        
        sk.setTrangThaiSK("Đã hủy");
        sk.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));
        suKienRepository.save(sk);
    }

    @Transactional
    public void thietLapSanKhau(String maSK, ThietLapSanKhauDTO dto) {
        System.out.println("DEBUG: Starting thietLapSanKhau for " + maSK);
        SuKien sk = suKienRepository.findById(maSK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + maSK));

        try {
            // 1. Delete existing if any (simplification for reset map)
            List<Ve> oldVe = veRepository.findByMaSK(maSK);
            System.out.println("DEBUG: Found " + oldVe.size() + " old tickets");
            List<Ghe> oldGhe = gheRepository.findByMaSK(maSK);
            System.out.println("DEBUG: Found " + oldGhe.size() + " old seats");
            List<KhuVuc> oldKhuVuc = khuVucRepository.findByMaSK(maSK);
            System.out.println("DEBUG: Found " + oldKhuVuc.size() + " old zones");

            // Delete in correct order to avoid FK constraint violations: Ve -> Ghe -> KhuVuc
            veRepository.deleteAll(oldVe);
            gheRepository.deleteAll(oldGhe);
            khuVucRepository.deleteAll(oldKhuVuc);
            System.out.println("DEBUG: Deleted old data");
        } catch (Exception e) {
            System.out.println("DEBUG: Error during deletion: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        int totalTickets = 0;

        List<KhuVuc> dsKhuVuc = new ArrayList<>();
        List<Ghe> dsGhe = new ArrayList<>();
        List<Ve> dsVe = new ArrayList<>();

        char[] rowLabels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        if (dto.getDanhSachKhuVuc() != null) {
            for (int k = 0; k < dto.getDanhSachKhuVuc().size(); k++) {
                ThietLapSanKhauDTO.KhuVucDTO kvDto = dto.getDanhSachKhuVuc().get(k);

                String maKhuVuc = "KV-" + System.currentTimeMillis() + "-" + k;
                KhuVuc kv = new KhuVuc(maKhuVuc, kvDto.getTenKhuVuc(), kvDto.getGiaVe(), kvDto.getSoHang(), kvDto.getSoGheMoiHang(), kvDto.getMauSac(), maSK);
                dsKhuVuc.add(kv);

                for (int i = 0; i < kvDto.getSoHang(); i++) {
                    String rowName = String.valueOf(rowLabels[i % rowLabels.length]);
                    if (i >= rowLabels.length) rowName = rowName + (i / rowLabels.length);

                    for (int j = 1; j <= kvDto.getSoGheMoiHang(); j++) {
                        String maGhe = maKhuVuc + "-" + rowName + j;
                        String tenGhe = rowName + j;

                        Ghe ghe = new Ghe(maGhe, tenGhe, "Trống", maKhuVuc, maSK);
                        dsGhe.add(ghe);

                        Ve ve = new Ve();
                        ve.setMaVe("VE-" + UUID.randomUUID().toString().substring(0, 8) + "-" + maGhe);
                        ve.setMaQR(UUID.randomUUID().toString());
                        ve.setGiaVe(kvDto.getGiaVe());
                        ve.setTrangThaiVe("Chưa bán");
                        ve.setThoiGianPhat(new Timestamp(System.currentTimeMillis()));
                        ve.setMaSK(maSK);
                        ve.setMaGhe(maGhe);
                        ve.setMaDonHang(null);
                        dsVe.add(ve);

                        totalTickets++;
                    }
                }
            }
        }

        khuVucRepository.saveAll(dsKhuVuc);
        gheRepository.saveAll(dsGhe);
        veRepository.saveAll(dsVe);

        sk.setTongSoVe(totalTickets);
        sk.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));
        suKienRepository.save(sk);
        System.out.println("DEBUG: Finished thietLapSanKhau for " + maSK + ". Total tickets: " + totalTickets);
    }
}

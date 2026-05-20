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
import java.util.ArrayList;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SuKienService {

    private static final Set<String> TRANG_THAI_HOP_LE = Set.of(
            "Chưa mở bán",
            "Đang mở bán",
            "Đã kết thúc",
            "Đã hủy",
            "Tạm ngưng"
    );

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
            String clean = timeStr.trim()
                    .replace("T", " ")
                    .replace("Z", "");

            if (clean.contains(".")) {
                clean = clean.split("\\.")[0];
            }

            if (clean.length() == 16) {
                clean += ":00";
            }

            if (clean.length() > 19) {
                clean = clean.substring(0, 19);
            }

            return Timestamp.valueOf(clean);
        } catch (Exception e) {
            System.err.println("Không thể parse timestamp: " + timeStr + " - " + e.getMessage());
            return null;
        }
    }

    private Timestamp parseTimestampOrThrow(String fieldName, String timeStr) {
        Timestamp parsed = parseTimestamp(timeStr);
        if (timeStr != null && !timeStr.isBlank() && parsed == null) {
            throw new RuntimeException(fieldName + " không hợp lệ. Vui lòng nhập đúng định dạng ngày giờ.");
        }
        return parsed;
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private void validateTrangThai(String trangThai) {
        if (trangThai == null || !TRANG_THAI_HOP_LE.contains(trangThai)) {
            throw new RuntimeException("Trạng thái sự kiện không hợp lệ.");
        }
    }

    private void validateThoiGian(Timestamp batDau, Timestamp ketThuc, Timestamp moBan, Timestamp dongBan) {
        if (batDau != null && ketThuc != null && !ketThuc.after(batDau)) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        if (moBan != null && dongBan != null && moBan.after(dongBan)) {
            throw new RuntimeException("Thời gian mở bán phải trước hoặc bằng thời gian đóng bán.");
        }

        if (dongBan != null && batDau != null && dongBan.after(batDau)) {
            throw new RuntimeException("Thời gian đóng bán phải trước hoặc bằng thời gian bắt đầu.");
        }
    }

    public SuKien taoSuKien(SuKienDTO dto) {
        if (dto.getTenSK() == null || dto.getTenSK().trim().isEmpty()) {
            throw new RuntimeException("Tên sự kiện không được để trống.");
        }

        Timestamp batDau = parseTimestampOrThrow("Thời gian bắt đầu", dto.getThoiGianBatDau());
        Timestamp ketThuc = parseTimestampOrThrow("Thời gian kết thúc", dto.getThoiGianKetThuc());
        Timestamp moBan = parseTimestampOrThrow("Thời gian mở bán", dto.getThoiGianMoBan());
        Timestamp dongBan = parseTimestampOrThrow("Thời gian đóng bán", dto.getThoiGianDongBan());
        String trangThai = normalizeNullable(dto.getTrangThaiSK());
        if (trangThai == null) {
            trangThai = "Chưa mở bán";
        }

        validateTrangThai(trangThai);
        validateThoiGian(batDau, ketThuc, moBan, dongBan);

        SuKien sk = new SuKien();

        String maSK = normalizeNullable(dto.getMaSK());
        if (maSK == null) {
            sk.setMaSK("SK-" + System.currentTimeMillis());
        } else {
            if (suKienRepository.existsById(maSK)) {
                throw new RuntimeException("Mã sự kiện đã tồn tại!");
            }
            sk.setMaSK(maSK);
        }

        sk.setTenSK(dto.getTenSK().trim());
        sk.setMoTa(dto.getMoTa());
        sk.setHinhAnh(dto.getHinhAnh());
        sk.setHinhAnhThumb(dto.getHinhAnhThumb());
        sk.setMoTaNgan(dto.getMoTaNgan());
        sk.setTags(dto.getTags());

        sk.setThoiGianBatDau(batDau);
        sk.setThoiGianKetThuc(ketThuc);
        sk.setThoiGianMoBan(moBan);
        sk.setThoiGianDongBan(dongBan);

        sk.setTongSoVe(dto.getTongSoVe() == null ? 0 : dto.getTongSoVe());
        sk.setSoVeDaBan(0);
        sk.setTrangThaiSK(trangThai);
        sk.setThoiGianTao(new Timestamp(System.currentTimeMillis()));

        sk.setMaLoaiSK(normalizeNullable(dto.getMaLoaiSK()));
        sk.setMaDiaDiem(normalizeNullable(dto.getMaDiaDiem()));
        sk.setMaNV(normalizeNullable(dto.getMaNV()));

        SuKien savedSk = suKienRepository.save(sk);

        // We no longer auto generate tickets here. It is moved to thietLapSanKhau.

        return savedSk;
    }

    public SuKien capNhatSuKien(String maSK, SuKienDTO dto) {
        if (dto.getTenSK() == null || dto.getTenSK().trim().isEmpty()) {
            throw new RuntimeException("Tên sự kiện không được để trống.");
        }

        Timestamp batDau = parseTimestampOrThrow("Thời gian bắt đầu", dto.getThoiGianBatDau());
        Timestamp ketThuc = parseTimestampOrThrow("Thời gian kết thúc", dto.getThoiGianKetThuc());
        Timestamp moBan = parseTimestampOrThrow("Thời gian mở bán", dto.getThoiGianMoBan());
        Timestamp dongBan = parseTimestampOrThrow("Thời gian đóng bán", dto.getThoiGianDongBan());
        String trangThai = normalizeNullable(dto.getTrangThaiSK());

        validateTrangThai(trangThai);
        validateThoiGian(batDau, ketThuc, moBan, dongBan);

        SuKien sk = suKienRepository.findById(maSK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + maSK));

        String dtoMaSK = normalizeNullable(dto.getMaSK());
        if (dtoMaSK != null && !dtoMaSK.equals(maSK)) {
            throw new RuntimeException("Không được thay đổi mã sự kiện.");
        }

        sk.setTenSK(dto.getTenSK().trim());
        sk.setMoTa(dto.getMoTa());
        sk.setHinhAnh(dto.getHinhAnh());
        sk.setHinhAnhThumb(dto.getHinhAnhThumb());
        sk.setMoTaNgan(dto.getMoTaNgan());
        sk.setTags(dto.getTags());

        sk.setThoiGianBatDau(batDau);
        sk.setThoiGianKetThuc(ketThuc);
        sk.setThoiGianMoBan(moBan);
        sk.setThoiGianDongBan(dongBan);

        // Không cập nhật TongSoVe/SoVeDaBan ở route sửa thông tin sự kiện.
        // Hai số này phải đến từ sơ đồ ghế/vé hoặc nghiệp vụ bán vé.
        sk.setTrangThaiSK(trangThai);
        
        sk.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));

        sk.setMaLoaiSK(normalizeNullable(dto.getMaLoaiSK()));
        sk.setMaDiaDiem(normalizeNullable(dto.getMaDiaDiem()));
        sk.setMaNV(normalizeNullable(dto.getMaNV()));

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

        List<Ve> oldVe = veRepository.findByMaSK(maSK);
        boolean hasSoldTickets = oldVe.stream().anyMatch(v -> v.getMaDonHang() != null && !v.getMaDonHang().trim().isEmpty());
        if (hasSoldTickets) {
            throw new RuntimeException("Không thể thiết lập lại sơ đồ ghế vì sự kiện đã phát sinh vé bán.");
        }

        try {
            System.out.println("DEBUG: Found " + oldVe.size() + " old tickets (none sold)");
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

        char[] rowLabels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        if (dto.getDanhSachKhuVuc() != null) {
            for (int k = 0; k < dto.getDanhSachKhuVuc().size(); k++) {
                ThietLapSanKhauDTO.KhuVucDTO kvDto = dto.getDanhSachKhuVuc().get(k);

                String maKhuVuc = maSK + "-KV" + (k + 1);
                int soGheToiDa = kvDto.getSoHang() * kvDto.getSoGheMoiHang();
                int soVeToiDaPerKH = kvDto.getSoVeToiDaPerKH() != null ? kvDto.getSoVeToiDaPerKH() : 4;
                
                KhuVuc kv = new KhuVuc(maKhuVuc, kvDto.getTenKhuVuc(), kvDto.getMauSacHienThi(), soGheToiDa, 0, soVeToiDaPerKH, kvDto.getGiaVe(), "Đang bán", maSK);
                kv.setSoHang(kvDto.getSoHang());
                kv.setSoGheMoiHang(kvDto.getSoGheMoiHang());
                dsKhuVuc.add(kv);

                for (int i = 0; i < kvDto.getSoHang(); i++) {
                    String rowName = String.valueOf(rowLabels[i % rowLabels.length]);
                    if (i >= rowLabels.length) rowName = rowName + (i / rowLabels.length);

                    for (int j = 1; j <= kvDto.getSoGheMoiHang(); j++) {
                        String tenGhe = rowName + String.format("%02d", j);
                        String maGhe = maSK + "-" + maKhuVuc + "-" + tenGhe;

                        Ghe ghe = new Ghe(maGhe, tenGhe, rowName, j, "Trống", maKhuVuc, maSK);
                        dsGhe.add(ghe);

                        totalTickets++;
                    }
                }
            }
        }

        khuVucRepository.saveAll(dsKhuVuc);
        gheRepository.saveAll(dsGhe);

        sk.setTongSoVe(totalTickets);
        sk.setCapNhatLanCuoi(new Timestamp(System.currentTimeMillis()));
        suKienRepository.save(sk);
        System.out.println("DEBUG: Finished thietLapSanKhau for " + maSK + ". Total tickets: " + totalTickets);
    }
}

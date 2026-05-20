package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.*;
import com.dede.ticketsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketValidationService {

    private final VeService veService;
    private final VeRepository veRepository;
    private final LichSuSoatVeRepository lichSuSoatVeRepository;
    private final GheRepository gheRepository;
    private final KhuVucRepository khuVucRepository;
    private final DonHangRepository donHangRepository;
    private final KhachHangRepository khachHangRepository;

    public TicketValidationService(VeService veService, VeRepository veRepository,
                                   LichSuSoatVeRepository lichSuSoatVeRepository, GheRepository gheRepository,
                                   KhuVucRepository khuVucRepository, DonHangRepository donHangRepository,
                                   KhachHangRepository khachHangRepository) {
        this.veService = veService;
        this.veRepository = veRepository;
        this.lichSuSoatVeRepository = lichSuSoatVeRepository;
        this.gheRepository = gheRepository;
        this.khuVucRepository = khuVucRepository;
        this.donHangRepository = donHangRepository;
        this.khachHangRepository = khachHangRepository;
    }

    @Transactional
    public ValidationResult validateQr(String qrPayloadOrCode, String maSK, String maNV, String congSoat, String nguonDuLieu) {
        return validateQr(qrPayloadOrCode, maSK, maNV, congSoat, nguonDuLieu, null);
    }

    @Transactional
    public ValidationResult validateQr(String qrPayloadOrCode, String maSK, String maNV, String congSoat, String nguonDuLieu, Timestamp thoiGianQuetParam) {
        long startTime = System.currentTimeMillis();
        Timestamp thoiGianQuet = thoiGianQuetParam != null ? thoiGianQuetParam : new Timestamp(startTime);

        // 1. Tìm vé theo payload hoặc code
        Optional<Ve> veOpt = veService.parsePayloadAndFindVe(qrPayloadOrCode);

        if (veOpt.isEmpty()) {
            // Không tìm thấy vé
            String status = "Vé không tìm thấy";
            
            LichSuSoatVe l = new LichSuSoatVe();
            l.setMaLichSu("LS-" + UUID.randomUUID().toString().substring(0, 8) + "-" + System.currentTimeMillis());
            l.setThoiGianQuet(thoiGianQuet);
            l.setKetQuaQuet(status);
            l.setCongSoat(congSoat);
            l.setNguonDuLieu(nguonDuLieu);
            l.setDaDongBo("Y");
            l.setThoiGianDongBo(new Timestamp(System.currentTimeMillis()));
            l.setMaVe(null);
            l.setMaNV(maNV);

            lichSuSoatVeRepository.save(l);

            long duration = System.currentTimeMillis() - startTime;
            return new ValidationResult(false, status, null, null, null, null, duration);
        }

        Ve ve = veOpt.get();

        // 2. Khóa dòng dữ liệu của Vé bằng PESSIMISTIC_WRITE để tránh race condition
        Optional<Ve> lockedVeOpt = veRepository.findByMaVeWithLock(ve.getMaVe());
        Ve lockedVe = lockedVeOpt.orElse(ve);

        // 3. Lấy thông tin chi tiết (ghế, khu vực, người mua)
        String seatName = null;
        String zoneName = null;
        String ticketOwner = null;

        if (lockedVe.getMaGhe() != null) {
            Ghe ghe = gheRepository.findById(lockedVe.getMaGhe()).orElse(null);
            if (ghe != null) {
                seatName = ghe.getTenGhe();
                KhuVuc kv = khuVucRepository.findById(ghe.getMaKhuVuc()).orElse(null);
                if (kv != null) {
                    zoneName = kv.getTenKhuVuc();
                }
            }
        }

        if (lockedVe.getMaDonHang() != null) {
            DonHang dh = donHangRepository.findById(lockedVe.getMaDonHang()).orElse(null);
            if (dh != null && dh.getMaKH() != null) {
                KhachHang kh = khachHangRepository.findById(dh.getMaKH()).orElse(null);
                if (kh != null) {
                    ticketOwner = kh.getHoTen();
                }
            }
        }

        // 4. Các bước logic xác thực
        String status;
        boolean success = false;

        if (lockedVe.getMaSK() == null || !lockedVe.getMaSK().equals(maSK)) {
            // Sai sự kiện
            status = "Sai sự kiện";
        } else if ("Đã sử dụng".equals(lockedVe.getTrangThaiVe())) {
            // Vé đã được sử dụng
            status = "Vé đã sử dụng";
        } else if ("Đã hủy".equals(lockedVe.getTrangThaiVe())) {
            // Vé đã hủy được coi là "Vé giả" do ràng buộc check constraint CHK_LSSV_KetQua
            status = "Vé giả";
        } else {
            // Vé hợp lệ
            status = "Hợp lệ";
            success = true;

            // Cập nhật trạng thái vé và thời gian sử dụng
            lockedVe.setTrangThaiVe("Đã sử dụng");
            // Offline sync thì dùng thời gian quét làm thời gian sử dụng thực tế
            lockedVe.setThoiGianSuDung(thoiGianQuet);
            veRepository.save(lockedVe);
        }

        // 5. Ghi lịch sử soát vé
        LichSuSoatVe l = new LichSuSoatVe();
        l.setMaLichSu("LS-" + UUID.randomUUID().toString().substring(0, 8) + "-" + System.currentTimeMillis());
        l.setThoiGianQuet(thoiGianQuet);
        l.setKetQuaQuet(status);
        l.setCongSoat(congSoat);
        l.setNguonDuLieu(nguonDuLieu);
        l.setDaDongBo("Y");
        l.setThoiGianDongBo(new Timestamp(System.currentTimeMillis()));
        l.setMaVe(lockedVe.getMaVe());
        l.setMaNV(maNV);

        lichSuSoatVeRepository.save(l);

        long duration = System.currentTimeMillis() - startTime;
        return new ValidationResult(success, status, lockedVe.getMaVe(), seatName, zoneName, ticketOwner, duration);
    }

    @Transactional
    public void resetTestData() {
        // Reset VE_SK001_A01_001
        veRepository.findById("VE_SK001_A01_001").ifPresent(v -> {
            v.setTrangThaiVe("Chưa sử dụng");
            v.setThoiGianSuDung(null);
            veRepository.save(v);
        });
        // Reset VE_SK001_VIP01_001
        veRepository.findById("VE_SK001_VIP01_001").ifPresent(v -> {
            v.setTrangThaiVe("Chưa sử dụng");
            v.setThoiGianSuDung(null);
            veRepository.save(v);
        });
        // Xóa lịch sử soát vé
        lichSuSoatVeRepository.deleteAll();
    }
}

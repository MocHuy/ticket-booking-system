package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.Ve;
import com.dede.ticketsystem.model.VeDTO;
import com.dede.ticketsystem.repository.VeRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VeService {

    private final VeRepository veRepository;

    public VeService(VeRepository veRepository) {
        this.veRepository = veRepository;
    }

    public List<Ve> layTatCa() {
        return veRepository.findAll();
    }

    public List<Ve> timKiem(String keyword, String trangThai) {
        return veRepository.search(keyword, trangThai);
    }

    public Optional<Ve> timTheoMa(String maVe) {
        return veRepository.findById(maVe);
    }

    public Ve taoVe(VeDTO dto) {
        Ve ve = new Ve();
        
        // Auto-generate MaVe if not provided
        if (dto.getMaVe() == null || dto.getMaVe().trim().isEmpty()) {
            ve.setMaVe("VE-" + System.currentTimeMillis());
        } else {
            if (veRepository.existsById(dto.getMaVe())) {
                throw new RuntimeException("Mã vé đã tồn tại!");
            }
            ve.setMaVe(dto.getMaVe());
        }

        // Auto-generate QR code if not provided
        if (dto.getMaQR() == null || dto.getMaQR().trim().isEmpty()) {
            ve.setMaQR(UUID.randomUUID().toString());
        } else {
            if (veRepository.existsByMaQR(dto.getMaQR())) {
                throw new RuntimeException("Mã QR đã tồn tại!");
            }
            ve.setMaQR(dto.getMaQR());
        }

        ve.setGiaVe(dto.getGiaVe());
        ve.setTrangThaiVe(dto.getTrangThaiVe() == null ? "Chưa sử dụng" : dto.getTrangThaiVe());
        ve.setThoiGianPhat(new Timestamp(System.currentTimeMillis()));
        
        if (dto.getThoiGianSuDung() != null && !dto.getThoiGianSuDung().isBlank()) {
            ve.setThoiGianSuDung(parseTimestampOrThrow("Thời gian sử dụng", dto.getThoiGianSuDung()));
        }
        
        ve.setMaDonHang(dto.getMaDonHang() != null && dto.getMaDonHang().isBlank() ? null : dto.getMaDonHang());
        ve.setMaGhe(dto.getMaGhe() != null && dto.getMaGhe().isBlank() ? null : dto.getMaGhe());
        ve.setMaSK(dto.getMaSK() != null && dto.getMaSK().isBlank() ? null : dto.getMaSK());

        return veRepository.save(ve);
    }

    public Ve capNhatVe(String maVe, VeDTO dto) {
        Ve ve = veRepository.findById(maVe)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé: " + maVe));

        if (dto.getGiaVe() != null) ve.setGiaVe(dto.getGiaVe());
        if (dto.getTrangThaiVe() != null) ve.setTrangThaiVe(dto.getTrangThaiVe());
        
        if (dto.getThoiGianSuDung() != null && !dto.getThoiGianSuDung().isBlank()) {
            ve.setThoiGianSuDung(parseTimestampOrThrow("Thời gian sử dụng", dto.getThoiGianSuDung()));
        }
        
        if (dto.getMaDonHang() != null) ve.setMaDonHang(dto.getMaDonHang().isBlank() ? null : dto.getMaDonHang());
        if (dto.getMaGhe() != null) ve.setMaGhe(dto.getMaGhe().isBlank() ? null : dto.getMaGhe());
        if (dto.getMaSK() != null) ve.setMaSK(dto.getMaSK().isBlank() ? null : dto.getMaSK());
        
        // Update QR Code only if explicitly provided and different
        if (dto.getMaQR() != null && !dto.getMaQR().isBlank() && !dto.getMaQR().equals(ve.getMaQR())) {
            if (veRepository.existsByMaQR(dto.getMaQR())) {
                throw new RuntimeException("Mã QR đã tồn tại trên một vé khác!");
            }
            ve.setMaQR(dto.getMaQR());
        }

        return veRepository.save(ve);
    }

    public void huyVe(String maVe) {
        Ve ve = veRepository.findById(maVe)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé: " + maVe));
        
        ve.setTrangThaiVe("Đã hủy");
        veRepository.save(ve);
    }

    public List<Ve> layVeCuaKhachHang(String maKH) {
        return veRepository.findByMaKH(maKH);
    }

    public Optional<Ve> parsePayloadAndFindVe(String payloadOrCode) {
        return parsePayloadAndFindVe(payloadOrCode, false);
    }

    public Optional<Ve> parsePayloadAndFindVeWithLock(String payloadOrCode) {
        return parsePayloadAndFindVe(payloadOrCode, true);
    }

    private Optional<Ve> parsePayloadAndFindVe(String payloadOrCode, boolean withLock) {
        if (payloadOrCode == null || payloadOrCode.trim().isEmpty()) {
            return Optional.empty();
        }

        String input = payloadOrCode.trim();

        if (input.startsWith("TICKET|")) {
            String[] parts = input.split("\\|");
            String parsedMaVe = null;
            String parsedMaQR = null;
            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length != 2) {
                    continue;
                }
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if ("maVe".equals(key)) {
                    parsedMaVe = value;
                } else if ("maQR".equals(key)) {
                    parsedMaQR = value;
                }
            }

            if (parsedMaVe != null && !parsedMaVe.trim().isEmpty()) {
                Optional<Ve> veOpt = findByMaVe(parsedMaVe.trim(), withLock);
                if (veOpt.isPresent()) {
                    return veOpt;
                }
            }

            if (parsedMaQR != null && !parsedMaQR.trim().isEmpty()) {
                Optional<Ve> veOpt = findByMaQR(parsedMaQR.trim(), withLock);
                if (veOpt.isPresent()) {
                    return veOpt;
                }
            }

            return Optional.empty();
        }

        Optional<Ve> veByQR = findByMaQR(input, withLock);
        if (veByQR.isPresent()) {
            return veByQR;
        }

        return findByMaVe(input, withLock);
    }

    private Optional<Ve> findByMaVe(String maVe, boolean withLock) {
        return withLock ? veRepository.findByMaVeWithLock(maVe) : veRepository.findById(maVe);
    }

    private Optional<Ve> findByMaQR(String maQR, boolean withLock) {
        return withLock ? veRepository.findByMaQRWithLock(maQR) : veRepository.findByMaQR(maQR);
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
}

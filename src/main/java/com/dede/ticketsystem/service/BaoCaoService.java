package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.BaoCaoSuKienDTO;
import com.dede.ticketsystem.model.BaoCaoTongQuanDTO;
import com.dede.ticketsystem.model.HanhViKhachHangDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BaoCaoService {

    private final JdbcTemplate jdbcTemplate;

    public BaoCaoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Lấy báo cáo tổng quan toàn hệ thống dựa trên bộ lọc
     */
    public BaoCaoTongQuanDTO getBaoCaoTongQuan(String maSK, Timestamp tuNgay, Timestamp denNgay, String trangThaiDonHang) {
        BaoCaoTongQuanDTO dto = new BaoCaoTongQuanDTO();

        // 1. Tổng doanh thu toàn hệ thống = SUM(DONHANG.ThanhTien) của các đơn có TrangThaiDonHang = 'Đã thanh toán'
        String sqlDoanhThu = "SELECT COALESCE(SUM(dh.ThanhTien), 0) FROM DONHANG dh WHERE dh.TrangThaiDonHang = 'Đã thanh toán' ";
        List<Object> paramsDT = new ArrayList<>();
        if (maSK != null && !maSK.trim().isEmpty()) {
            sqlDoanhThu += "AND dh.MaDonHang IN (SELECT DISTINCT v.MaDonHang FROM VE v WHERE v.MaSK = ?) ";
            paramsDT.add(maSK);
        }
        if (tuNgay != null) {
            sqlDoanhThu += "AND dh.ThoiGianDat >= ? ";
            paramsDT.add(tuNgay);
        }
        if (denNgay != null) {
            sqlDoanhThu += "AND dh.ThoiGianDat <= ? ";
            paramsDT.add(denNgay);
        }
        BigDecimal tongDoanhThu = jdbcTemplate.queryForObject(sqlDoanhThu, BigDecimal.class, paramsDT.toArray());
        dto.setTongDoanhThu(tongDoanhThu);

        // 2. Tổng vé đã bán = count VE thuộc đơn "Đã thanh toán"
        String sqlVeDaBan = "SELECT COUNT(v.MaVe) FROM VE v JOIN DONHANG dh ON v.MaDonHang = dh.MaDonHang WHERE dh.TrangThaiDonHang = 'Đã thanh toán' ";
        List<Object> paramsVe = new ArrayList<>();
        if (maSK != null && !maSK.trim().isEmpty()) {
            sqlVeDaBan += "AND v.MaSK = ? ";
            paramsVe.add(maSK);
        }
        if (tuNgay != null) {
            sqlVeDaBan += "AND dh.ThoiGianDat >= ? ";
            paramsVe.add(tuNgay);
        }
        if (denNgay != null) {
            sqlVeDaBan += "AND dh.ThoiGianDat <= ? ";
            paramsVe.add(denNgay);
        }
        Long tongVeDaBan = jdbcTemplate.queryForObject(sqlVeDaBan, Long.class, paramsVe.toArray());
        dto.setTongVeDaBan(tongVeDaBan != null ? tongVeDaBan : 0L);

        // 3. Tổng số đơn theo trạng thái (chỉ áp dụng bộ lọc sự kiện và ngày)
        dto.setDonChoThanhToan(countDonHangByStatus("Chờ thanh toán", maSK, tuNgay, denNgay));
        dto.setDonDaThanhToan(countDonHangByStatus("Đã thanh toán", maSK, tuNgay, denNgay));
        dto.setDonDaHuy(countDonHangByStatus("Đã hủy", maSK, tuNgay, denNgay));

        // 4. Tỷ lệ thanh toán thành công = số giao dịch "Thành công" / tổng giao dịch * 100
        String sqlGDThanhCong = "SELECT COUNT(gd.MaGiaoDich) FROM GIAODICHTHANHTOAN gd JOIN DONHANG dh ON gd.MaDonHang = dh.MaDonHang WHERE gd.TrangThaiGD = 'Thành công' ";
        String sqlGDTong = "SELECT COUNT(gd.MaGiaoDich) FROM GIAODICHTHANHTOAN gd JOIN DONHANG dh ON gd.MaDonHang = dh.MaDonHang WHERE 1=1 ";
        List<Object> paramsGD = new ArrayList<>();
        
        if (maSK != null && !maSK.trim().isEmpty()) {
            String subquery = "AND dh.MaDonHang IN (SELECT DISTINCT v.MaDonHang FROM VE v WHERE v.MaSK = ?) ";
            sqlGDThanhCong += subquery;
            sqlGDTong += subquery;
            paramsGD.add(maSK);
        }
        if (tuNgay != null) {
            String filterDate = "AND gd.ThoiGianThucHien >= ? ";
            sqlGDThanhCong += filterDate;
            sqlGDTong += filterDate;
            paramsGD.add(tuNgay);
        }
        if (denNgay != null) {
            String filterDate = "AND gd.ThoiGianThucHien <= ? ";
            sqlGDThanhCong += filterDate;
            sqlGDTong += filterDate;
            paramsGD.add(denNgay);
        }
        if (trangThaiDonHang != null && !trangThaiDonHang.trim().isEmpty()) {
            String filterStatus = "AND dh.TrangThaiDonHang = ? ";
            sqlGDThanhCong += filterStatus;
            sqlGDTong += filterStatus;
            paramsGD.add(trangThaiDonHang);
        }

        Long countGDThanhCong = jdbcTemplate.queryForObject(sqlGDThanhCong, Long.class, paramsGD.toArray());
        Long countGDTong = jdbcTemplate.queryForObject(sqlGDTong, Long.class, paramsGD.toArray());

        double tyLeThanhToanThanhCong = 0.0;
        if (countGDTong != null && countGDTong > 0) {
            tyLeThanhToanThanhCong = (countGDThanhCong != null ? countGDThanhCong : 0.0) / countGDTong * 100.0;
        }
        dto.setTyLeThanhToanThanhCong(tyLeThanhToanThanhCong);

        // 5. Tỷ lệ lấp đầy trung bình = SoVeDaBan / TongSoVe * 100 (tính gộp trên các sự kiện tương ứng)
        String sqlTongVeSK = "SELECT COALESCE(SUM(TongSoVe), 0) FROM SUKIEN WHERE 1=1 ";
        List<Object> paramsSK = new ArrayList<>();
        if (maSK != null && !maSK.trim().isEmpty()) {
            sqlTongVeSK += "AND MaSK = ? ";
            paramsSK.add(maSK);
        }
        Long totalTongSoVe = jdbcTemplate.queryForObject(sqlTongVeSK, Long.class, paramsSK.toArray());

        double tyLeLapDayTB = 0.0;
        if (totalTongSoVe != null && totalTongSoVe > 0) {
            tyLeLapDayTB = (double) dto.getTongVeDaBan() / totalTongSoVe * 100.0;
        }
        dto.setTyLeLapDayTB(tyLeLapDayTB);

        // 6. Sự kiện bán chạy nhất = Sự kiện có lượng vé bán ra nhiều nhất trong khoảng thời gian lọc
        String sqlBestSeller = "SELECT sk.TenSK, COUNT(v.MaVe) AS SoldCount " +
                "FROM VE v " +
                "JOIN DONHANG dh ON v.MaDonHang = dh.MaDonHang " +
                "JOIN SUKIEN sk ON v.MaSK = sk.MaSK " +
                "WHERE dh.TrangThaiDonHang = 'Đã thanh toán' ";
        List<Object> paramsBest = new ArrayList<>();
        if (maSK != null && !maSK.trim().isEmpty()) {
            sqlBestSeller += "AND v.MaSK = ? ";
            paramsBest.add(maSK);
        }
        if (tuNgay != null) {
            sqlBestSeller += "AND dh.ThoiGianDat >= ? ";
            paramsBest.add(tuNgay);
        }
        if (denNgay != null) {
            sqlBestSeller += "AND dh.ThoiGianDat <= ? ";
            paramsBest.add(denNgay);
        }
        sqlBestSeller += "GROUP BY sk.TenSK ORDER BY SoldCount DESC ";

        List<Map<String, Object>> bestResult = jdbcTemplate.queryForList(sqlBestSeller, paramsBest.toArray());
        if (!bestResult.isEmpty()) {
            Map<String, Object> topEvent = bestResult.get(0);
            dto.setSuKienBanChayNhat(String.valueOf(topEvent.get("TENSK")));
        } else {
            dto.setSuKienBanChayNhat("Chưa có");
        }

        return dto;
    }

    /**
     * Đếm số lượng đơn hàng theo trạng thái và bộ lọc
     */
    private long countDonHangByStatus(String status, String maSK, Timestamp tuNgay, Timestamp denNgay) {
        String sql = "SELECT COUNT(dh.MaDonHang) FROM DONHANG dh WHERE dh.TrangThaiDonHang = ? ";
        List<Object> params = new ArrayList<>();
        params.add(status);

        if (maSK != null && !maSK.trim().isEmpty()) {
            sql += "AND dh.MaDonHang IN (SELECT DISTINCT v.MaDonHang FROM VE v WHERE v.MaSK = ?) ";
            params.add(maSK);
        }
        if (tuNgay != null) {
            sql += "AND dh.ThoiGianDat >= ? ";
            params.add(tuNgay);
        }
        if (denNgay != null) {
            sql += "AND dh.ThoiGianDat <= ? ";
            params.add(denNgay);
        }
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * Bảng báo cáo theo từng sự kiện
     */
    public List<BaoCaoSuKienDTO> getBaoCaoSuKien(String maSK, Timestamp tuNgay, Timestamp denNgay, String trangThaiDonHang) {
        String sqlSK = "SELECT MaSK, TenSK, TongSoVe FROM SUKIEN WHERE 1=1 ";
        List<Object> paramsSK = new ArrayList<>();
        if (maSK != null && !maSK.trim().isEmpty()) {
            sqlSK += "AND MaSK = ? ";
            paramsSK.add(maSK);
        }
        sqlSK += "ORDER BY TenSK ASC";

        List<Map<String, Object>> listSK = jdbcTemplate.queryForList(sqlSK, paramsSK.toArray());
        List<BaoCaoSuKienDTO> reportList = new ArrayList<>();

        for (Map<String, Object> row : listSK) {
            String skCode = String.valueOf(row.get("MASK"));
            String skName = String.valueOf(row.get("TENSK"));
            Number tongVeNum = (Number) row.get("TONGSOVE");
            long tongSoVe = tongVeNum != null ? tongVeNum.longValue() : 0L;

            BaoCaoSuKienDTO eventReport = new BaoCaoSuKienDTO();
            eventReport.setMaSK(skCode);
            eventReport.setTenSK(skName);
            eventReport.setTongSoVe(tongSoVe);

            // 1. Số vé đã bán trong kỳ = count VE thuộc đơn "Đã thanh toán"
            String sqlSold = "SELECT COUNT(v.MaVe) FROM VE v JOIN DONHANG dh ON v.MaDonHang = dh.MaDonHang " +
                    "WHERE v.MaSK = ? AND dh.TrangThaiDonHang = 'Đã thanh toán' ";
            List<Object> paramsSold = new ArrayList<>();
            paramsSold.add(skCode);
            if (tuNgay != null) {
                sqlSold += "AND dh.ThoiGianDat >= ? ";
                paramsSold.add(tuNgay);
            }
            if (denNgay != null) {
                sqlSold += "AND dh.ThoiGianDat <= ? ";
                paramsSold.add(denNgay);
            }
            if (trangThaiDonHang != null && !trangThaiDonHang.trim().isEmpty()) {
                sqlSold += "AND dh.TrangThaiDonHang = ? ";
                paramsSold.add(trangThaiDonHang);
            }
            Long soVeDaBan = jdbcTemplate.queryForObject(sqlSold, Long.class, paramsSold.toArray());
            long veDaBan = soVeDaBan != null ? soVeDaBan : 0L;
            eventReport.setSoVeDaBan(veDaBan);

            // 2. Số vé còn lại
            long veConLai = tongSoVe - veDaBan;
            eventReport.setSoVeConLai(veConLai < 0 ? 0L : veConLai);

            // 3. Doanh thu theo sự kiện: Dùng SUM(VE.GiaVe) Join VE -> DONHANG chỉ lấy vé thuộc đơn "Đã thanh toán"
            String sqlRevenue = "SELECT COALESCE(SUM(v.GiaVe), 0) FROM VE v JOIN DONHANG dh ON v.MaDonHang = dh.MaDonHang " +
                    "WHERE v.MaSK = ? AND dh.TrangThaiDonHang = 'Đã thanh toán' ";
            List<Object> paramsRev = new ArrayList<>();
            paramsRev.add(skCode);
            if (tuNgay != null) {
                sqlRevenue += "AND dh.ThoiGianDat >= ? ";
                paramsRev.add(tuNgay);
            }
            if (denNgay != null) {
                sqlRevenue += "AND dh.ThoiGianDat <= ? ";
                paramsRev.add(denNgay);
            }
            if (trangThaiDonHang != null && !trangThaiDonHang.trim().isEmpty()) {
                sqlRevenue += "AND dh.TrangThaiDonHang = ? ";
                paramsRev.add(trangThaiDonHang);
            }
            BigDecimal doanhThu = jdbcTemplate.queryForObject(sqlRevenue, BigDecimal.class, paramsRev.toArray());
            eventReport.setDoanhThu(doanhThu);

            // 4. Tỷ lệ lấp đầy = SoVeDaBan / TongSoVe * 100. Nếu TongSoVe = 0 thì trả 0.
            double tyLeLapDay = 0.0;
            if (tongSoVe > 0) {
                tyLeLapDay = (double) veDaBan / tongSoVe * 100.0;
            }
            eventReport.setTyLeLapDay(tyLeLapDay);

            // 5. Nhật ký hành vi: lượt xem, lượt click, lượt bỏ giỏ hàng
            eventReport.setLuotXem(countHanhHanhDong(skCode, "XEM_SK", tuNgay, denNgay));
            eventReport.setLuotClick(countHanhHanhDong(skCode, "CLICK_DAT_VE", tuNgay, denNgay));
            eventReport.setLuotBoGioHang(countHanhHanhDong(skCode, "BO_GIO_HANG", tuNgay, denNgay));

            // 6. Tỷ lệ chuyển đổi = số đơn "Đã thanh toán" của sự kiện / số lượt "XEM_SK" * 100. Nếu XEM_SK = 0 thì trả 0.
            String sqlPaidOrdersCount = "SELECT COUNT(DISTINCT dh.MaDonHang) FROM DONHANG dh JOIN VE v ON v.MaDonHang = dh.MaDonHang " +
                    "WHERE v.MaSK = ? AND dh.TrangThaiDonHang = 'Đã thanh toán' ";
            List<Object> paramsPaid = new ArrayList<>();
            paramsPaid.add(skCode);
            if (tuNgay != null) {
                sqlPaidOrdersCount += "AND dh.ThoiGianDat >= ? ";
                paramsPaid.add(tuNgay);
            }
            if (denNgay != null) {
                sqlPaidOrdersCount += "AND dh.ThoiGianDat <= ? ";
                paramsPaid.add(denNgay);
            }
            if (trangThaiDonHang != null && !trangThaiDonHang.trim().isEmpty()) {
                sqlPaidOrdersCount += "AND dh.TrangThaiDonHang = ? ";
                paramsPaid.add(trangThaiDonHang);
            }
            Long countPaidOrders = jdbcTemplate.queryForObject(sqlPaidOrdersCount, Long.class, paramsPaid.toArray());
            long paidOrders = countPaidOrders != null ? countPaidOrders : 0L;

            double tyLeChuyenDoi = 0.0;
            if (eventReport.getLuotXem() > 0) {
                tyLeChuyenDoi = (double) paidOrders / eventReport.getLuotXem() * 100.0;
            }
            eventReport.setTyLeChuyenDoi(tyLeChuyenDoi);

            reportList.add(eventReport);
        }

        return reportList;
    }

    /**
     * Đếm số lượt hành động cụ thể theo sự kiện và khoảng thời gian
     */
    private long countHanhHanhDong(String maSK, String loaiHanhDong, Timestamp tuNgay, Timestamp denNgay) {
        String sql = "SELECT COUNT(MaLog) FROM LOG_HANH_VI WHERE MaSK = ? AND LoaiHanhDong = ? ";
        List<Object> params = new ArrayList<>();
        params.add(maSK);
        params.add(loaiHanhDong);

        if (tuNgay != null) {
            sql += "AND ThoiGian >= ? ";
            params.add(tuNgay);
        }
        if (denNgay != null) {
            sql += "AND ThoiGian <= ? ";
            params.add(denNgay);
        }
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * Danh sách 50 hành vi khách hàng gần nhất
     */
    public List<HanhViKhachHangDTO> getRecentHanhViKhachHang(String maSK, Timestamp tuNgay, Timestamp denNgay) {
        String sql = "SELECT * FROM ( " +
                "  SELECT l.MaLog, l.LoaiHanhDong, l.MaSK, sk.TenSK, l.ThoiGian, l.MaKH, nd.TenTaiKhoan, l.ThietBi " +
                "  FROM LOG_HANH_VI l " +
                "  LEFT JOIN SUKIEN sk ON l.MaSK = sk.MaSK " +
                "  LEFT JOIN KHACHHANG kh ON l.MaKH = kh.MaKH " +
                "  LEFT JOIN NGUOIDUNG nd ON kh.MaND = nd.MaND " +
                "  WHERE 1=1 ";
        List<Object> params = new ArrayList<>();

        if (maSK != null && !maSK.trim().isEmpty()) {
            sql += "AND l.MaSK = ? ";
            params.add(maSK);
        }
        if (tuNgay != null) {
            sql += "AND l.ThoiGian >= ? ";
            params.add(tuNgay);
        }
        if (denNgay != null) {
            sql += "AND l.ThoiGian <= ? ";
            params.add(denNgay);
        }

        sql += "  ORDER BY l.ThoiGian DESC " +
                ") WHERE ROWNUM <= 50";

        List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(sql, params.toArray());
        List<HanhViKhachHangDTO> logs = new ArrayList<>();

        for (Map<String, Object> row : queryResult) {
            HanhViKhachHangDTO logDto = new HanhViKhachHangDTO();
            logDto.setMaLog(String.valueOf(row.get("MALOG")));
            logDto.setLoaiHanhDong(String.valueOf(row.get("LOAIHANHDONG")));
            logDto.setMaSK(row.get("MASK") != null ? String.valueOf(row.get("MASK")) : "");
            logDto.setTenSK(row.get("TENSK") != null ? String.valueOf(row.get("TENSK")) : "Hệ thống");
            logDto.setThoiGian((Timestamp) row.get("THOIGIAN"));
            logDto.setMaKH(row.get("MAKH") != null ? String.valueOf(row.get("MAKH")) : null);
            logDto.setTenKH(row.get("TENTAIKHOAN") != null ? String.valueOf(row.get("TENTAIKHOAN")) : "Khách vãng lai");
            logDto.setThietBi(row.get("THIETBI") != null ? String.valueOf(row.get("THIETBI")) : "Web");

            logs.add(logDto);
        }

        return logs;
    }
}

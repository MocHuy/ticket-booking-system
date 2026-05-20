package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.model.DonHang;
import com.dede.ticketsystem.model.Ve;
import com.dede.ticketsystem.model.KhuVuc;
import com.dede.ticketsystem.model.SuKien;
import com.dede.ticketsystem.model.GiaoDichThanhToan;
import com.dede.ticketsystem.repository.GheRepository;
import com.dede.ticketsystem.repository.DonHangRepository;
import com.dede.ticketsystem.repository.VeRepository;
import com.dede.ticketsystem.repository.KhuVucRepository;
import com.dede.ticketsystem.repository.SuKienRepository;
import com.dede.ticketsystem.repository.GiaoDichThanhToanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class BookingService {

    private final GheRepository gheRepository;
    private final DonHangRepository donHangRepository;
    private final VeRepository veRepository;
    private final KhuVucRepository khuVucRepository;
    private final SuKienRepository suKienRepository;
    private final GiaoDichThanhToanRepository giaoDichThanhToanRepository;

    public BookingService(GheRepository gheRepository, 
                          DonHangRepository donHangRepository, 
                          VeRepository veRepository, 
                          KhuVucRepository khuVucRepository,
                          SuKienRepository suKienRepository,
                          GiaoDichThanhToanRepository giaoDichThanhToanRepository) {
        this.gheRepository = gheRepository;
        this.donHangRepository = donHangRepository;
        this.veRepository = veRepository;
        this.khuVucRepository = khuVucRepository;
        this.suKienRepository = suKienRepository;
        this.giaoDichThanhToanRepository = giaoDichThanhToanRepository;
    }

    @Transactional
    public String lockSeats(List<String> maGheList, String maSK, String maKH) {
        if (maGheList == null || maGheList.isEmpty()) {
            throw new RuntimeException("Danh sách ghế chọn không được để trống!");
        }

        // Sort danh sách maGhe tăng dần để giảm nguy cơ deadlock
        java.util.Collections.sort(maGheList);

        // Dùng PESSIMISTIC_WRITE để khóa các bản ghi GHENGOI
        List<Ghe> gheList = gheRepository.findAllByIdWithLock(maGheList);

        // Kiểm tra số lượng ghế load được phải bằng số lượng maGhe request
        if (gheList.size() != maGheList.size()) {
            throw new RuntimeException("Một số ghế không tồn tại hoặc không hợp lệ!");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Validate backend
        SuKien suKien = suKienRepository.findById(maSK)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

        // Sự kiện có TrangThaiSK = "Đang mở bán"
        if (!"Đang mở bán".equalsIgnoreCase(suKien.getTrangThaiSK())) {
            throw new RuntimeException("Sự kiện hiện không trong trạng thái mở bán!");
        }

        // Thời gian hiện tại nằm giữa ThoiGianMoBan và ThoiGianDongBan
        if (suKien.getThoiGianMoBan() != null && now.before(suKien.getThoiGianMoBan())) {
            throw new RuntimeException("Thời gian mở bán vé sự kiện chưa bắt đầu!");
        }
        if (suKien.getThoiGianDongBan() != null && now.after(suKien.getThoiGianDongBan())) {
            throw new RuntimeException("Thời gian bán vé sự kiện đã kết thúc!");
        }

        // Validate trạng thái của từng ghế
        for (Ghe ghe : gheList) {
            if (!maSK.equals(ghe.getMaSK())) {
                throw new RuntimeException("Ghế " + ghe.getTenGhe() + " không thuộc sự kiện đang chọn!");
            }
            if (!"Trống".equals(ghe.getTrangThaiGhe())) {
                throw new RuntimeException("Ghế " + ghe.getTenGhe() + " đã được bán hoặc đang được chọn bởi người khác!");
            }
        }

        // Kiểm tra SoVeToiDaPerKH theo từng khu vực và tổng số vé khách đã mua trước đó cho sự kiện/khu vực
        java.util.Map<String, Integer> gheChonTheoKhuVuc = new java.util.HashMap<>();
        for (Ghe ghe : gheList) {
            gheChonTheoKhuVuc.put(ghe.getMaKhuVuc(), gheChonTheoKhuVuc.getOrDefault(ghe.getMaKhuVuc(), 0) + 1);
        }

        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        for (java.util.Map.Entry<String, Integer> entry : gheChonTheoKhuVuc.entrySet()) {
            String maKhuVuc = entry.getKey();
            int soGheChon = entry.getValue();

            KhuVuc kv = khuVucRepository.findById(maKhuVuc)
                    .orElseThrow(() -> new RuntimeException("Khu vực không tồn tại!"));

            int maxVe = (kv.getSoVeToiDaPerKH() != null && kv.getSoVeToiDaPerKH() > 0) ? kv.getSoVeToiDaPerKH() : 4;

            long veDaMua = veRepository.countBoughtTicketsByKHAndSKAndKhuVuc(maKH, maSK, maKhuVuc);

            if (soGheChon + veDaMua > maxVe) {
                throw new RuntimeException("Bạn đã chọn hoặc mua tổng cộng " + (soGheChon + veDaMua) + 
                        " vé ở khu vực " + kv.getTenKhuVuc() + ". Giới hạn tối đa là " + maxVe + " vé!");
            }

            java.math.BigDecimal giaVe = (kv.getGiaVe() != null) ? kv.getGiaVe() : java.math.BigDecimal.ZERO;
            tongTien = tongTien.add(giaVe.multiply(new java.math.BigDecimal(soGheChon)));
        }

        // Tạo DONHANG trạng thái "Chờ thanh toán"
        String maDonHang = "DH-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Timestamp thoiGianHetHan = new Timestamp(now.getTime() + 10 * 60 * 1000); // now + 10 phút

        DonHang dh = new DonHang();
        dh.setMaDonHang(maDonHang);
        dh.setSoDonHang(maDonHang);
        dh.setTongTien(tongTien);
        dh.setThanhTien(tongTien);
        dh.setTrangThaiDonHang("Chờ thanh toán");
        dh.setThoiGianDat(now);
        dh.setThoiGianHetHan(thoiGianHetHan);
        dh.setCapNhatLanCuoi(now);
        dh.setMaKH(maKH);

        donHangRepository.save(dh);

        // Cập nhật GHENGOI
        for (Ghe ghe : gheList) {
            ghe.setTrangThaiGhe("Đang chọn");
            ghe.setThoiGianKhoaTam(thoiGianHetHan);
            ghe.setMaPhienKhoa(maDonHang);
        }
        gheRepository.saveAll(gheList);

        return maDonHang;
    }

    @Transactional
    public void processCheckout(String orderId, String maKH, String paymentMethod, String simulateResult) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Khóa DONHANG bằng PESSIMISTIC_WRITE trước khi xử lý
        DonHang dh = donHangRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        // Kiểm tra đơn hàng thuộc maKH hiện tại
        if (!dh.getMaKH().equals(maKH)) {
            throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này!");
        }

        // Nếu đơn đã "Đã thanh toán"
        if ("Đã thanh toán".equalsIgnoreCase(dh.getTrangThaiDonHang())) {
            // Không tạo vé thêm lần nữa
            List<Ve> veDaCo = veRepository.findByMaDonHang(orderId);
            if (!veDaCo.isEmpty()) {
                // Trả về thông báo đơn đã thanh toán
                return;
            }
            issueTicketsAfterPaymentSuccess(orderId);
            return;
        }

        // Nếu đơn đã "Đã hủy"
        if ("Đã hủy".equalsIgnoreCase(dh.getTrangThaiDonHang())) {
            throw new RuntimeException("Đơn hàng đã bị hủy và không thể checkout!");
        }

        // Nếu đơn đã hết hạn
        if (dh.getThoiGianHetHan() != null && now.after(dh.getThoiGianHetHan())) {
            dh.setTrangThaiDonHang("Đã hủy");
            dh.setCapNhatLanCuoi(now);
            donHangRepository.save(dh);

            releaseSeats(orderId);

            throw new RuntimeException("Đơn hàng đã hết hạn thanh toán!");
        }

        // Ghi GIAODICHTHANHTOAN
        GiaoDichThanhToan gd = new GiaoDichThanhToan();
        gd.setMaGiaoDich("GD-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        gd.setSoTienThanhToan(dh.getThanhTien());
        gd.setPhuongThucTT(paymentMethod);
        gd.setThoiGianThucHien(now);
        gd.setMaDonHang(orderId);
        gd.setLanThuLai(0);

        if ("Thành công".equalsIgnoreCase(simulateResult)) {
            gd.setTrangThaiGD("Thành công");
            gd.setMaGiaoDichBenThu3("MOCK-SUCCESS-" + java.util.UUID.randomUUID().toString().substring(0, 8));
            giaoDichThanhToanRepository.save(gd);

            // Cập nhật DONHANG = "Đã thanh toán"
            dh.setTrangThaiDonHang("Đã thanh toán");
            dh.setCapNhatLanCuoi(now);
            donHangRepository.save(dh);

            // Sinh vé và cập nhật trạng thái ghế
            issueTicketsAfterPaymentSuccess(orderId);
        } else {
            gd.setTrangThaiGD("Thất bại");
            gd.setGhiChuLoi("Giao dịch giả lập thất bại.");
            giaoDichThanhToanRepository.save(gd);

            // Hướng B: Giữ DONHANG ở "Chờ thanh toán" nếu còn thời gian để retry
            throw new RuntimeException("Thanh toán thất bại! Vui lòng thử lại.");
        }
    }

    @Transactional
    public void issueTicketsAfterPaymentSuccess(String orderId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Lấy danh sách ghế bằng findByMaPhienKhoa(orderId)
        List<Ghe> gheList = gheRepository.findByMaPhienKhoa(orderId);

        // Dùng danh sách đó để tạo vé
        for (Ghe ghe : gheList) {
            // Chỉ tạo vé khi chưa tồn tại vé cho cặp MaDonHang + MaGhe
            if (!veRepository.existsByMaDonHangAndMaGhe(orderId, ghe.getMaGhe())) {
                KhuVuc kv = khuVucRepository.findById(ghe.getMaKhuVuc())
                        .orElseThrow(() -> new RuntimeException("Khu vực của ghế không tồn tại!"));

                // Sinh MaVe unique
                String maVe;
                do {
                    maVe = "VE-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                } while (veRepository.existsByMaVe(maVe));

                // Sinh MaQR unique
                String maQR;
                do {
                    maQR = "QR-" + java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
                } while (veRepository.existsByMaQR(maQR));

                Ve ve = new Ve();
                ve.setMaVe(maVe);
                ve.setMaQR(maQR);
                ve.setGiaVe(kv.getGiaVe() != null ? kv.getGiaVe() : java.math.BigDecimal.ZERO);
                ve.setTrangThaiVe("Chưa sử dụng");
                ve.setThoiGianPhat(now);
                ve.setMaSK(ghe.getMaSK());
                ve.setMaGhe(ghe.getMaGhe());
                ve.setMaDonHang(orderId);

                veRepository.save(ve);

                // Cập nhật SUKIEN.SoVeDaBan
                SuKien sk = suKienRepository.findById(ghe.getMaSK()).orElse(null);
                if (sk != null) {
                    sk.setSoVeDaBan((sk.getSoVeDaBan() != null ? sk.getSoVeDaBan() : 0) + 1);
                    suKienRepository.save(sk);
                }

                // Cập nhật KHUVUC.SoGheDaBan
                kv.setSoGheDaBan((kv.getSoGheDaBan() != null ? kv.getSoGheDaBan() : 0) + 1);
                khuVucRepository.save(kv);
            }
        }

        // Sau khi tạo vé xong mới clear ThoiGianKhoaTam và MaPhienKhoa, và cập nhật GHENGOI = "Đã bán"
        for (Ghe ghe : gheList) {
            ghe.setTrangThaiGhe("Đã bán");
            ghe.setThoiGianKhoaTam(null);
            ghe.setMaPhienKhoa(null);
        }
        gheRepository.saveAll(gheList);
    }

    @Transactional
    public void cancelOrder(String orderId, String maKH) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Khóa DONHANG bằng PESSIMISTIC_WRITE trước khi xử lý
        DonHang dh = donHangRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        // Kiểm tra đơn hàng thuộc maKH hiện tại
        if (!dh.getMaKH().equals(maKH)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        // Trạng thái còn "Chờ thanh toán"
        if (!"Chờ thanh toán".equalsIgnoreCase(dh.getTrangThaiDonHang())) {
            throw new RuntimeException("Đơn hàng không ở trạng thái chờ thanh toán để có thể hủy!");
        }

        // Đổi trạng thái sang Đã hủy
        dh.setTrangThaiDonHang("Đã hủy");
        dh.setCapNhatLanCuoi(now);
        donHangRepository.save(dh);

        // Nhả ghế
        releaseSeats(orderId);
    }

    @Transactional
    public void releaseSeats(String orderId) {
        List<Ghe> gheList = gheRepository.findByMaPhienKhoa(orderId);
        for (Ghe ghe : gheList) {
            ghe.setTrangThaiGhe("Trống");
            ghe.setThoiGianKhoaTam(null);
            ghe.setMaPhienKhoa(null);
        }
        gheRepository.saveAll(gheList);
    }
}

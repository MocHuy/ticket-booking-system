-- ============================================================
-- TRIGGERS - Hệ thống Quản lý Sự kiện & Bán vé trực tuyến
-- ITPJ2602 | Nhóm SPRING
-- ============================================================


-- ---------------------------------------------------------------
-- TRG01: TRG_KiemTraOverbooking
-- BEFORE INSERT on VE - Chặn tuyệt đối tình trạng overbooking
-- Tái sử dụng tư duy từ TRG_KiemTraTruocMoPhien (workspace gốc)
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_KiemTraOverbooking
BEFORE INSERT ON VE
FOR EACH ROW
DECLARE
    v_SoGheDaBan    NUMBER;
    v_SoGheToiDa    NUMBER;
BEGIN
    SELECT SoGheDaBan, SoGheToiDa INTO v_SoGheDaBan, v_SoGheToiDa
    FROM KHUVUC
    WHERE MaKhuVuc = (SELECT MaKhuVuc FROM GHENGOI WHERE MaGhe = :NEW.MaGhe);

    IF v_SoGheDaBan >= v_SoGheToiDa THEN
        RAISE_APPLICATION_ERROR(-20001,
            'Lỗi: Khu vực đã hết vé! Không thể tạo thêm vé mới.');
    END IF;
END TRG_KiemTraOverbooking;
/


-- ---------------------------------------------------------------
-- TRG02: TRG_ChanVeTrungMaQR
-- BEFORE INSERT on VE - Đảm bảo tuyệt đối không có 2 vé cùng mã QR
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ChanVeTrungMaQR
BEFORE INSERT ON VE
FOR EACH ROW
DECLARE
    v_Count NUMBER;
BEGIN
    IF :NEW.MaQR IS NOT NULL THEN
        SELECT COUNT(*) INTO v_Count FROM VE WHERE MaQR = :NEW.MaQR;
        IF v_Count > 0 THEN
            RAISE_APPLICATION_ERROR(-20002,
                'Lỗi nghiêm trọng: Mã QR đã tồn tại trong hệ thống! Vé không được phép trùng mã.');
        END IF;
    END IF;
END TRG_ChanVeTrungMaQR;
/


-- ---------------------------------------------------------------
-- TRG03: TRG_CapNhatTrangThaiGheSauBan
-- AFTER INSERT on VE - Cập nhật trạng thái ghế và đếm ghế bán
-- Tương tự TRG_CapNhatTrangThaiKhongGian (workspace gốc)
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CapNhatTrangThaiGheSauBan
AFTER INSERT ON VE
FOR EACH ROW
BEGIN
    -- Cập nhật ghế sang "Đã bán"
    UPDATE GHENGOI
    SET TrangThaiGhe = 'Đã bán', ThoiGianKhoaTam = NULL, MaPhienKhoa = NULL
    WHERE MaGhe = :NEW.MaGhe;

    -- Cập nhật số ghế đã bán trong khu vực
    UPDATE KHUVUC
    SET SoGheDaBan = SoGheDaBan + 1
    WHERE MaKhuVuc = (SELECT MaKhuVuc FROM GHENGOI WHERE MaGhe = :NEW.MaGhe);

    -- Cập nhật tổng vé đã bán của sự kiện
    UPDATE SUKIEN
    SET SoVeDaBan = SoVeDaBan + 1, CapNhatLanCuoi = SYSTIMESTAMP
    WHERE MaSK = :NEW.MaSK;
END TRG_CapNhatTrangThaiGheSauBan;
/


-- ---------------------------------------------------------------
-- TRG04: TRG_GiaiPhongGheSauHuyVe
-- AFTER UPDATE on VE - Khi vé bị hủy, trả ghế về trạng thái Trống
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_GiaiPhongGheSauHuyVe
AFTER UPDATE OF TrangThaiVe ON VE
FOR EACH ROW
WHEN (NEW.TrangThaiVe = 'Đã hủy' AND OLD.TrangThaiVe != 'Đã hủy')
BEGIN
    UPDATE GHENGOI
    SET TrangThaiGhe = 'Trống', ThoiGianKhoaTam = NULL, MaPhienKhoa = NULL
    WHERE MaGhe = :NEW.MaGhe;

    UPDATE KHUVUC
    SET SoGheDaBan = GREATEST(SoGheDaBan - 1, 0)
    WHERE MaKhuVuc = (SELECT MaKhuVuc FROM GHENGOI WHERE MaGhe = :NEW.MaGhe);

    UPDATE SUKIEN
    SET SoVeDaBan = GREATEST(SoVeDaBan - 1, 0), CapNhatLanCuoi = SYSTIMESTAMP
    WHERE MaSK = :NEW.MaSK;
END TRG_GiaiPhongGheSauHuyVe;
/


-- ---------------------------------------------------------------
-- TRG05: TRG_TichLuyChiTieuKhachHang
-- AFTER INSERT/UPDATE on GIAODICHTHANHQUAN
-- Tích lũy chi tiêu khi giao dịch thành công
-- Tái sử dụng 100% logic từ TRG_TichLuyChiTieuKhachHang (workspace gốc)
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_TichLuyChiTieuKhachHang
AFTER INSERT OR UPDATE ON GIAODICHTHANHQUAN
FOR EACH ROW
DECLARE
    v_MaKH VARCHAR2(50);
BEGIN
    IF :NEW.TrangThaiGD = 'Thành công' AND
       (INSERTING OR (UPDATING AND NVL(:OLD.TrangThaiGD, '') != 'Thành công')) THEN

        BEGIN
            SELECT MaKH INTO v_MaKH FROM DONHANG WHERE MaDonHang = :NEW.MaDonHang;

            IF v_MaKH IS NOT NULL THEN
                UPDATE KHACHHANG
                SET TongChiTieu = NVL(TongChiTieu, 0) + :NEW.SoTienThanhToan,
                    CapNhatLanCuoi = SYSTIMESTAMP
                WHERE MaKH = v_MaKH;
            END IF;
        EXCEPTION WHEN NO_DATA_FOUND THEN NULL;
        END;
    END IF;
END TRG_TichLuyChiTieuKhachHang;
/


-- ---------------------------------------------------------------
-- TRG06: TRG_TuDongThangHangThanhVien
-- BEFORE UPDATE OF TongChiTieu on KHACHHANG
-- Tự động thăng hạng thành viên khi đạt ngưỡng chi tiêu
-- Tái sử dụng 100% logic từ TRG_TuDongThangHang (workspace gốc)
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_TuDongThangHangThanhVien
BEFORE UPDATE OF TongChiTieu ON KHACHHANG
FOR EACH ROW
DECLARE
    v_MaHangMoi VARCHAR2(50);
BEGIN
    IF :NEW.TongChiTieu > NVL(:OLD.TongChiTieu, 0) THEN
        BEGIN
            SELECT MaHangThanhVien INTO v_MaHangMoi
            FROM (
                SELECT MaHangThanhVien
                FROM HANGTHANHVIEN
                WHERE TongChiTieuToiThieu <= :NEW.TongChiTieu
                ORDER BY TongChiTieuToiThieu DESC
            )
            WHERE ROWNUM = 1;

            IF v_MaHangMoi IS NOT NULL AND NVL(:OLD.MaHangThanhVien, ' ') != v_MaHangMoi THEN
                :NEW.MaHangThanhVien := v_MaHangMoi;
            END IF;
        EXCEPTION WHEN NO_DATA_FOUND THEN NULL;
        END;
    END IF;
END TRG_TuDongThangHangThanhVien;
/


-- ---------------------------------------------------------------
-- TRG07: TRG_ChanThanhToanDonHangHetHan
-- BEFORE INSERT on GIAODICHTHANHQUAN - Chặn thanh toán cho đơn đã hết hạn
-- Tương tự TRG_ChanThemDichVu_PhienDong (workspace gốc)
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ChanThanhToanDonHangHetHan
BEFORE INSERT ON GIAODICHTHANHQUAN
FOR EACH ROW
DECLARE
    v_TrangThai     VARCHAR2(50);
    v_ThoiGianHetHan TIMESTAMP;
BEGIN
    SELECT TrangThaiDonHang, ThoiGianHetHan
    INTO v_TrangThai, v_ThoiGianHetHan
    FROM DONHANG WHERE MaDonHang = :NEW.MaDonHang;

    IF v_TrangThai != 'Chờ thanh toán' THEN
        RAISE_APPLICATION_ERROR(-20003,
            'Lỗi: Đơn hàng này không ở trạng thái chờ thanh toán (Trạng thái hiện tại: '
            || v_TrangThai || ')');
    END IF;

    IF SYSTIMESTAMP > v_ThoiGianHetHan THEN
        RAISE_APPLICATION_ERROR(-20004,
            'Lỗi: Đơn hàng đã hết hạn giữ vé. Vui lòng đặt lại từ đầu!');
    END IF;
END TRG_ChanThanhToanDonHangHetHan;
/


-- ---------------------------------------------------------------
-- TRG08: TRG_GhiNhatKyThayDoiQuyen
-- AFTER INSERT/DELETE on CHITIETVAITRO - Audit trail bảo mật
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_GhiNhatKyThayDoiQuyen
AFTER INSERT OR DELETE ON CHITIETVAITRO
FOR EACH ROW
DECLARE
    v_MaNK  VARCHAR2(50);
    v_HD    VARCHAR2(50);
    v_MaND  VARCHAR2(50);
    v_MaVT  VARCHAR2(50);
BEGIN
    IF INSERTING THEN
        v_HD := 'GAN_QUYEN'; v_MaND := :NEW.MaND; v_MaVT := :NEW.MaVaiTro;
    ELSE
        v_HD := 'HUY_QUYEN'; v_MaND := :OLD.MaND; v_MaVT := :OLD.MaVaiTro;
    END IF;

    v_MaNK := 'NK_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3');
    INSERT INTO NHATKYQUYENHANTRO (MaNhatKy, HanhDong, MaVaiTroMoi, ThoiGianThucHien, MaND_BiThayDoi)
    VALUES (v_MaNK, v_HD, v_MaVT, SYSTIMESTAMP, v_MaND);
END TRG_GhiNhatKyThayDoiQuyen;
/


-- ---------------------------------------------------------------
-- TRG09: TRG_CanhBaoSuKienTaiCao
-- AFTER UPDATE OF TrangThaiSK on SUKIEN
-- Cảnh báo khi số sự kiện mở bán đồng thời vượt ngưỡng
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CanhBaoSuKienTaiCao
AFTER UPDATE OF TrangThaiSK ON SUKIEN
FOR EACH ROW
WHEN (NEW.TrangThaiSK = 'Đang mở bán')
DECLARE
    v_SoSKDangBan NUMBER;
    v_Nguong      NUMBER := 5;  -- Ngưỡng cảnh báo: >5 sự kiện mở bán cùng lúc
BEGIN
    SELECT COUNT(*) INTO v_SoSKDangBan
    FROM SUKIEN WHERE TrangThaiSK = 'Đang mở bán';

    IF v_SoSKDangBan > v_Nguong THEN
        DBMS_OUTPUT.PUT_LINE('[CẢNH BÁO] Hiện có ' || v_SoSKDangBan
            || ' sự kiện đang mở bán đồng thời. Admin cần tăng cường tài nguyên máy chủ!');
    END IF;
END TRG_CanhBaoSuKienTaiCao;
/


-- ---------------------------------------------------------------
-- TRG10: TRG_PhatVeSauThanhToanThanhCong
-- AFTER UPDATE OF TrangThaiGD on GIAODICHTHANHQUAN
-- Phát vé (cập nhật ThoiGianPhat) ngay khi giao dịch thành công
-- ---------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PhatVeSauThanhToanThanhCong
AFTER UPDATE OF TrangThaiGD ON GIAODICHTHANHQUAN
FOR EACH ROW
WHEN (NEW.TrangThaiGD = 'Thành công' AND OLD.TrangThaiGD != 'Thành công')
BEGIN
    -- Cập nhật thời gian phát vé cho tất cả vé trong đơn hàng
    UPDATE VE
    SET ThoiGianPhat = SYSTIMESTAMP
    WHERE MaDonHang = :NEW.MaDonHang
      AND TrangThaiVe = 'Chưa sử dụng';

    -- Giảm số lượng đã dùng của phiếu giảm giá (nếu có)
    UPDATE PHIEUGIAMGIA
    SET SLDaDung = SLDaDung + 1
    WHERE MaPGG = (SELECT MaPGG FROM DONHANG WHERE MaDonHang = :NEW.MaDonHang AND MaPGG IS NOT NULL);
END TRG_PhatVeSauThanhToanThanhCong;
/

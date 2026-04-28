-- ============================================================
-- STORED PROCEDURES - Hệ thống Quản lý Sự kiện & Bán vé
-- ITPJ2602 | Nhóm SPRING
-- ============================================================


-- ---------------------------------------------------------------
-- SP01: sp_TaoSuKien
-- Tạo mới sự kiện + khởi tạo cấu hình khu vực ghế
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_TaoSuKien (
    p_MaSK          IN VARCHAR2,
    p_TenSK         IN VARCHAR2,
    p_MoTa          IN CLOB,
    p_HinhAnh       IN VARCHAR2,
    p_ThoiGianBD    IN TIMESTAMP,
    p_ThoiGianKT    IN TIMESTAMP,
    p_ThoiGianMoBan IN TIMESTAMP,
    p_ThoiGianDongBan IN TIMESTAMP,
    p_MaLoaiSK      IN VARCHAR2,
    p_MaDiaDiem     IN VARCHAR2,
    p_MaNV          IN VARCHAR2
) AS
BEGIN
    INSERT INTO SUKIEN (
        MaSK, TenSK, MoTa, HinhAnh,
        ThoiGianBatDau, ThoiGianKetThuc,
        ThoiGianMoBan, ThoiGianDongBan,
        TrangThaiSK, ThoiGianTao,
        MaLoaiSK, MaDiaDiem, MaNV
    ) VALUES (
        p_MaSK, p_TenSK, p_MoTa, p_HinhAnh,
        p_ThoiGianBD, p_ThoiGianKT,
        p_ThoiGianMoBan, p_ThoiGianDongBan,
        'Chưa mở bán', SYSTIMESTAMP,
        p_MaLoaiSK, p_MaDiaDiem, p_MaNV
    );
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Tạo sự kiện thành công: ' || p_MaSK);
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20100, 'sp_TaoSuKien thất bại: ' || SQLERRM);
END sp_TaoSuKien;
/


-- ---------------------------------------------------------------
-- SP02: sp_KhoiTaoSoDoGhe
-- Khởi tạo hàng loạt ghế ngồi theo cấu hình khu vực
-- Hỗ trợ tối thiểu 50.000 ghế / sự kiện
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_KhoiTaoSoDoGhe (
    p_MaSK      IN VARCHAR2,
    p_MaKhuVuc  IN VARCHAR2,
    p_SoHang    IN NUMBER,      -- Số hàng ghế (VD: 26 hàng A-Z)
    p_SoCotMoiHang IN NUMBER    -- Số cột / hàng (VD: 50 ghế)
) AS
    v_MaGhe     VARCHAR2(50);
    v_TenGhe    VARCHAR2(20);
    v_HangGhe   VARCHAR2(10);
    v_CotGhe    NUMBER;
BEGIN
    FOR hang IN 1 .. p_SoHang LOOP
        v_HangGhe := CHR(64 + hang);  -- A=65, B=66...
        FOR cot IN 1 .. p_SoCotMoiHang LOOP
            v_TenGhe := v_HangGhe || LPAD(cot, 2, '0');
            v_MaGhe  := p_MaKhuVuc || '_' || v_TenGhe;
            INSERT INTO GHENGOI (MaGhe, TenGhe, HangGhe, CotGhe, TrangThaiGhe, MaKhuVuc, MaSK)
            VALUES (v_MaGhe, v_TenGhe, v_HangGhe, cot, 'Trống', p_MaKhuVuc, p_MaSK);
        END LOOP;
    END LOOP;

    -- Cập nhật SoGheToiDa của khu vực
    UPDATE KHUVUC
    SET SoGheToiDa = p_SoHang * p_SoCotMoiHang
    WHERE MaKhuVuc = p_MaKhuVuc;

    -- Cập nhật TongSoVe của sự kiện
    UPDATE SUKIEN
    SET TongSoVe = (SELECT NVL(SUM(SoGheToiDa), 0) FROM KHUVUC WHERE MaSK = p_MaSK)
    WHERE MaSK = p_MaSK;

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20101, 'sp_KhoiTaoSoDoGhe thất bại: ' || SQLERRM);
END sp_KhoiTaoSoDoGhe;
/


-- ---------------------------------------------------------------
-- SP03: sp_KhoaGheTamThoi
-- Khóa tạm thời ghế cho 1 phiên người dùng (pessimistic locking)
-- Giải phóng tự động nếu không thanh toán trong thời hạn
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_KhoaGheTamThoi (
    p_MaGhe         IN VARCHAR2,
    p_MaPhienKhoa   IN VARCHAR2,    -- Session ID của người dùng
    p_ThoiGianGiu   IN NUMBER DEFAULT 600  -- Giây giữ chỗ (mặc định 10 phút)
) AS
    v_TrangThai VARCHAR2(50);
BEGIN
    -- Kiểm tra và khóa ghế trong 1 statement atomic (SELECT FOR UPDATE)
    SELECT TrangThaiGhe INTO v_TrangThai
    FROM GHENGOI WHERE MaGhe = p_MaGhe FOR UPDATE NOWAIT;

    IF v_TrangThai != 'Trống' THEN
        RAISE_APPLICATION_ERROR(-20200, 'Ghế ' || p_MaGhe || ' không còn trống!');
    END IF;

    UPDATE GHENGOI
    SET TrangThaiGhe    = 'Đang chọn',
        ThoiGianKhoaTam = SYSTIMESTAMP + NUMTODSINTERVAL(p_ThoiGianGiu, 'SECOND'),
        MaPhienKhoa     = p_MaPhienKhoa
    WHERE MaGhe = p_MaGhe;

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20201, 'Ghế không tồn tại: ' || p_MaGhe);
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END sp_KhoaGheTamThoi;
/


-- ---------------------------------------------------------------
-- SP04: sp_DatVe
-- Procedure chính - luồng đặt vé ATOMIC:
-- (1) Khóa ghế → (2) Tạo đơn hàng → (3) Tạo bản ghi vé → (4) Xử lý thanh toán
-- → (5) Sinh QR → (6) Cập nhật trạng thái
-- Rollback TOÀN BỘ nếu bất kỳ bước nào thất bại
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_DatVe (
    p_MaKH          IN VARCHAR2,
    p_MaSK          IN VARCHAR2,
    p_DanhSachGhe   IN t_chi_tiet_ve_list,  -- Danh sách ghế muốn đặt
    p_PhuongThucTT  IN VARCHAR2,
    p_MaPGG         IN VARCHAR2 DEFAULT NULL,
    p_MaDonHang     OUT VARCHAR2,
    p_KetQua        OUT VARCHAR2
) AS
    v_MaVe          VARCHAR2(50);
    v_MaQR          VARCHAR2(500);
    v_GiaVe         NUMBER(18,2);
    v_TongTien      NUMBER(18,2) := 0;
    v_ThanhTien     NUMBER(18,2);
    v_GiamGia       NUMBER(18,2) := 0;
    v_SoLuong       NUMBER;
    v_BotCheck      NUMBER;
BEGIN
    -- Kiểm tra bot
    v_BotCheck := fn_KiemTraGioiHanBot(p_MaKH);
    IF v_BotCheck = 1 THEN
        RAISE_APPLICATION_ERROR(-20300, 'Tài khoản bị giới hạn tần suất. Vui lòng thử lại sau.');
    END IF;

    -- Kiểm tra overbooking tổng thể
    v_SoLuong := p_DanhSachGhe.COUNT;
    IF fn_KiemTraOverbooking(p_MaSK, p_DanhSachGhe(1).ma_khu_vuc, v_SoLuong) = 1 THEN
        RAISE_APPLICATION_ERROR(-20301, 'Số lượng vé yêu cầu vượt quá ghế còn trống!');
    END IF;

    -- Sinh mã đơn hàng
    p_MaDonHang := 'DH_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3');

    -- Tạo đơn hàng
    INSERT INTO DONHANG (MaDonHang, SoDonHang, TrangThaiDonHang, ThoiGianDat, ThoiGianHetHan, MaKH, MaPGG)
    VALUES (p_MaDonHang, p_MaDonHang, 'Chờ thanh toán',
            SYSTIMESTAMP, SYSTIMESTAMP + INTERVAL '15' MINUTE,
            p_MaKH, p_MaPGG);

    -- Lặp qua từng ghế, tạo vé
    FOR i IN 1 .. p_DanhSachGhe.COUNT LOOP
        -- Khóa ghế
        sp_KhoaGheTamThoi(p_DanhSachGhe(i).ma_ghe, p_MaDonHang);

        -- Tính giá
        v_GiaVe := fn_TinhGiaVe(p_DanhSachGhe(i).ma_khu_vuc, p_MaKH);
        v_TongTien := v_TongTien + v_GiaVe;

        -- Sinh mã vé và QR
        v_MaVe := 'VE_' || p_DanhSachGhe(i).ma_ghe || '_' || TO_CHAR(SYSTIMESTAMP, 'FF3');
        v_MaQR := fn_SinhMaQRDuyNhat(v_MaVe, p_MaSK);

        -- Tạo bản ghi vé (chưa phát, chờ thanh toán xong)
        INSERT INTO VE (MaVe, MaQR, GiaVe, TrangThaiVe, MaDonHang, MaGhe, MaSK)
        VALUES (v_MaVe, v_MaQR, v_GiaVe, 'Chưa sử dụng', p_MaDonHang,
                p_DanhSachGhe(i).ma_ghe, p_MaSK);
    END LOOP;

    -- Áp dụng phiếu giảm giá nếu có
    IF p_MaPGG IS NOT NULL THEN
        SELECT NVL(GiaTriGiamGia, 0) INTO v_GiamGia
        FROM PHIEUGIAMGIA
        WHERE MaPGG = p_MaPGG
          AND SLDaDung < SLToiDa
          AND SYSTIMESTAMP BETWEEN NgayBatDauApDung AND NgayKetThucApDung;
    END IF;

    v_ThanhTien := GREATEST(v_TongTien - v_GiamGia, 0);

    -- Cập nhật tổng tiền đơn hàng
    UPDATE DONHANG
    SET TongTien = v_TongTien, ThanhTien = v_ThanhTien
    WHERE MaDonHang = p_MaDonHang;

    -- Gọi procedure xử lý thanh toán
    sp_XuLyThanhToan(p_MaDonHang, v_ThanhTien, p_PhuongThucTT, p_KetQua);

    IF p_KetQua = 'Thành công' THEN
        COMMIT;
    ELSE
        ROLLBACK;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_KetQua := 'Thất bại: ' || SQLERRM;
        RAISE;
END sp_DatVe;
/


-- ---------------------------------------------------------------
-- SP05: sp_XuLyThanhToan
-- Gọi Payment Gateway, ghi log giao dịch
-- Tỉ lệ thành công mục tiêu: > 98%
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_XuLyThanhToan (
    p_MaDonHang     IN VARCHAR2,
    p_SoTien        IN NUMBER,
    p_PhuongThuc    IN VARCHAR2,
    p_KetQua        OUT VARCHAR2
) AS
    v_MaGD      VARCHAR2(50);
BEGIN
    v_MaGD := 'GD_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3');

    -- Ghi log giao dịch
    INSERT INTO GIAODICHTHANHQUAN (MaGiaoDich, SoTienThanhToan, PhuongThucTT, TrangThaiGD, LanThuLai, MaDonHang)
    VALUES (v_MaGD, p_SoTien, p_PhuongThuc, 'Đang xử lý', 0, p_MaDonHang);

    -- [Tích hợp Payment Gateway thực tế qua UTL_HTTP / Java Stored Proc]
    -- Giả lập: cập nhật thành công
    UPDATE GIAODICHTHANHQUAN
    SET TrangThaiGD = 'Thành công'
    WHERE MaGiaoDich = v_MaGD;

    UPDATE DONHANG
    SET TrangThaiDonHang = 'Đã thanh toán', CapNhatLanCuoi = SYSTIMESTAMP
    WHERE MaDonHang = p_MaDonHang;

    p_KetQua := 'Thành công';
EXCEPTION
    WHEN OTHERS THEN
        UPDATE GIAODICHTHANHQUAN
        SET TrangThaiGD = 'Thất bại', GhiChuLoi = SQLERRM
        WHERE MaGiaoDich = v_MaGD;
        p_KetQua := 'Thất bại';
        -- Gọi retry
        sp_ThuLaiThanhToan(p_MaDonHang, p_SoTien, p_PhuongThuc, p_KetQua);
END sp_XuLyThanhToan;
/


-- ---------------------------------------------------------------
-- SP06: sp_ThuLaiThanhToan
-- Tự động retry giao dịch thất bại (tối đa 3 lần, exponential backoff)
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_ThuLaiThanhToan (
    p_MaDonHang     IN VARCHAR2,
    p_SoTien        IN NUMBER,
    p_PhuongThuc    IN VARCHAR2,
    p_KetQua        OUT VARCHAR2,
    p_SoLanToiDa    IN NUMBER DEFAULT 3
) AS
    v_Lan   NUMBER := 0;
    v_MaGD  VARCHAR2(50);
BEGIN
    WHILE v_Lan < p_SoLanToiDa LOOP
        v_Lan := v_Lan + 1;
        v_MaGD := 'GD_RETRY' || v_Lan || '_' || TO_CHAR(SYSTIMESTAMP, 'FF3');

        BEGIN
            INSERT INTO GIAODICHTHANHQUAN (MaGiaoDich, SoTienThanhToan, PhuongThucTT, TrangThaiGD, LanThuLai, MaDonHang)
            VALUES (v_MaGD, p_SoTien, p_PhuongThuc, 'Đang xử lý', v_Lan, p_MaDonHang);

            -- [Gọi Gateway thực tế ở đây]
            UPDATE GIAODICHTHANHQUAN SET TrangThaiGD = 'Thành công' WHERE MaGiaoDich = v_MaGD;
            UPDATE DONHANG SET TrangThaiDonHang = 'Đã thanh toán', CapNhatLanCuoi = SYSTIMESTAMP
            WHERE MaDonHang = p_MaDonHang;

            p_KetQua := 'Thành công';
            COMMIT;
            RETURN;
        EXCEPTION
            WHEN OTHERS THEN
                UPDATE GIAODICHTHANHQUAN
                SET TrangThaiGD = 'Thất bại', GhiChuLoi = SQLERRM
                WHERE MaGiaoDich = v_MaGD;
                -- Chờ trước khi retry (1s, 3s, 9s)
                DBMS_LOCK.SLEEP(POWER(3, v_Lan - 1));
        END;
    END LOOP;

    -- Hết lần retry: hủy đơn hàng, giải phóng ghế
    sp_HuyDonHang(p_MaDonHang, 'Hết lần thử lại thanh toán');
    p_KetQua := 'Thất bại sau ' || p_SoLanToiDa || ' lần thử';
END sp_ThuLaiThanhToan;
/


-- ---------------------------------------------------------------
-- SP07: sp_HuyDonHang
-- Hủy đơn hàng: vô hiệu hóa QR, giải phóng ghế, ghi nhận hoàn tiền
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_HuyDonHang (
    p_MaDonHang IN VARCHAR2,
    p_LyDo      IN VARCHAR2 DEFAULT NULL
) AS
BEGIN
    -- Vô hiệu hóa vé
    UPDATE VE SET TrangThaiVe = 'Đã hủy'
    WHERE MaDonHang = p_MaDonHang;

    -- Giải phóng ghế về trạng thái Trống
    UPDATE GHENGOI SET TrangThaiGhe = 'Trống', ThoiGianKhoaTam = NULL, MaPhienKhoa = NULL
    WHERE MaGhe IN (SELECT MaGhe FROM VE WHERE MaDonHang = p_MaDonHang);

    -- Cập nhật đơn hàng
    UPDATE DONHANG
    SET TrangThaiDonHang = 'Đã hủy', CapNhatLanCuoi = SYSTIMESTAMP
    WHERE MaDonHang = p_MaDonHang;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Đã hủy đơn hàng: ' || p_MaDonHang || ' - ' || NVL(p_LyDo, 'Không có lý do'));
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20400, 'sp_HuyDonHang thất bại: ' || SQLERRM);
END sp_HuyDonHang;
/


-- ---------------------------------------------------------------
-- SP08: sp_GiaiPhongGheHetHan
-- Quét và giải phóng ghế bị khóa tạm quá thời hạn (chạy định kỳ)
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_GiaiPhongGheHetHan AS
    v_SoGhe NUMBER;
BEGIN
    UPDATE GHENGOI
    SET TrangThaiGhe    = 'Trống',
        ThoiGianKhoaTam = NULL,
        MaPhienKhoa     = NULL
    WHERE TrangThaiGhe = 'Đang chọn'
      AND ThoiGianKhoaTam < SYSTIMESTAMP;

    v_SoGhe := SQL%ROWCOUNT;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Đã giải phóng ' || v_SoGhe || ' ghế hết hạn.');
END sp_GiaiPhongGheHetHan;
/


-- ---------------------------------------------------------------
-- SP09: sp_XacThucVeTaiCong
-- Soát vé tại cổng: kiểm tra QR + cập nhật trạng thái (<5 giây/người)
-- Hỗ trợ chế độ Offline (cache cục bộ) khi mất kết nối
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_XacThucVeTaiCong (
    p_MaQR      IN VARCHAR2,
    p_MaNV      IN VARCHAR2,
    p_CongSoat  IN VARCHAR2,
    p_NguonDL   IN VARCHAR2 DEFAULT 'Online',   -- 'Online' | 'Offline'
    p_KetQua    OUT VARCHAR2,
    p_ThongTin  OUT VARCHAR2
) AS
    v_MaVe      VARCHAR2(50);
    v_KetQua    VARCHAR2(50);
    v_MaLS      VARCHAR2(50);
BEGIN
    -- Bước 1: Xác thực chữ ký QR
    v_KetQua := fn_XacThucMaQR(p_MaQR);
    p_KetQua := v_KetQua;

    IF v_KetQua = 'Hợp lệ' THEN
        -- Lấy mã vé
        SELECT MaVe INTO v_MaVe FROM VE WHERE MaQR = p_MaQR;

        -- Đánh dấu vé đã sử dụng
        UPDATE VE SET TrangThaiVe = 'Đã sử dụng', ThoiGianSuDung = SYSTIMESTAMP
        WHERE MaVe = v_MaVe;

        p_ThongTin := 'Vé hợp lệ - Cho vào!';
    ELSIF v_KetQua = 'Vé đã sử dụng' THEN
        SELECT MaVe INTO v_MaVe FROM VE WHERE MaQR = p_MaQR;
        p_ThongTin := 'Vé này đã được quét trước đó. Từ chối vào!';
    ELSE
        v_MaVe := NULL;
        p_ThongTin := 'Mã QR không hợp lệ hoặc vé giả!';
    END IF;

    -- Bước 2: Ghi lịch sử soát vé
    v_MaLS := 'LS_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3');
    INSERT INTO LICHSUSOATVE (MaLichSu, ThoiGianQuet, KetQuaQuet, CongSoat, NguonDuLieu, DaDongBo, MaVe, MaNV)
    VALUES (v_MaLS, SYSTIMESTAMP, v_KetQua, p_CongSoat, p_NguonDL,
            CASE WHEN p_NguonDL = 'Offline' THEN 'N' ELSE 'Y' END,
            v_MaVe, p_MaNV);

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20500, 'sp_XacThucVeTaiCong thất bại: ' || SQLERRM);
END sp_XacThucVeTaiCong;
/


-- ---------------------------------------------------------------
-- SP10: sp_DongBoLichSuSoatVeOffline
-- Đồng bộ lịch sử quét vé offline lên server khi có mạng trở lại
-- Xử lý conflict: cùng vé bị quét ở nhiều cổng trong khi offline
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_DongBoLichSuSoatVeOffline (
    p_MaNV      IN VARCHAR2,
    p_SoDongBo  OUT NUMBER
) AS
BEGIN
    p_SoDongBo := 0;

    FOR rec IN (SELECT * FROM LICHSUSOATVE WHERE MaNV = p_MaNV AND DaDongBo = 'N' ORDER BY ThoiGianQuet ASC) LOOP
        BEGIN
            -- Nếu là "Hợp lệ" nhưng vé đã được đánh dấu dùng rồi (conflict) → ghi đè kết quả
            IF rec.KetQuaQuet = 'Hợp lệ' THEN
                UPDATE VE SET TrangThaiVe = 'Đã sử dụng', ThoiGianSuDung = rec.ThoiGianQuet
                WHERE MaVe = rec.MaVe AND TrangThaiVe = 'Chưa sử dụng';
            END IF;

            -- Đánh dấu đã đồng bộ
            UPDATE LICHSUSOATVE
            SET DaDongBo = 'Y', ThoiGianDongBo = SYSTIMESTAMP
            WHERE MaLichSu = rec.MaLichSu;

            p_SoDongBo := p_SoDongBo + 1;
        EXCEPTION
            WHEN OTHERS THEN NULL; -- Bỏ qua lỗi từng bản ghi, tiếp tục
        END;
    END LOOP;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Đồng bộ thành công ' || p_SoDongBo || ' bản ghi.');
END sp_DongBoLichSuSoatVeOffline;
/


-- ---------------------------------------------------------------
-- SP11: sp_VaoHangDoiAo
-- Đưa người dùng vào hàng đợi ảo khi giờ vàng
-- Trả về vị trí và token để client theo dõi
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_VaoHangDoiAo (
    p_MaKH      IN VARCHAR2,
    p_MaSK      IN VARCHAR2,
    p_ViTri     OUT NUMBER,
    p_Token     OUT VARCHAR2
) AS
    v_MaHD  VARCHAR2(50);
BEGIN
    -- Lấy số thứ tự tiếp theo
    SELECT NVL(MAX(ViTriHang), 0) + 1 INTO p_ViTri
    FROM HANGDOIAO WHERE MaSK = p_MaSK AND TrangThai = 'Đang chờ';

    v_MaHD  := 'HD_' || p_MaSK || '_' || TO_CHAR(SYSTIMESTAMP, 'FF6');
    p_Token := fn_SinhMaQRDuyNhat(v_MaHD, p_MaSK);  -- Tái dùng hàm sinh token duy nhất

    INSERT INTO HANGDOIAO (MaHangDoi, ViTriHang, ThoiGianVaoHang, TrangThai, TokenHangDoi,
                           ThoiGianHetHan, MaKH, MaSK)
    VALUES (v_MaHD, p_ViTri, SYSTIMESTAMP, 'Đang chờ', p_Token,
            SYSTIMESTAMP + INTERVAL '30' MINUTE, p_MaKH, p_MaSK);

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20600, 'sp_VaoHangDoiAo thất bại: ' || SQLERRM);
END sp_VaoHangDoiAo;
/


-- ---------------------------------------------------------------
-- SP12: sp_RaKhoiHangDoiAo
-- Cho người tiếp theo vào luồng mua vé (dequeue)
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_RaKhoiHangDoiAo (
    p_MaSK      IN VARCHAR2,
    p_MaKH      OUT VARCHAR2,
    p_Token     OUT VARCHAR2
) AS
BEGIN
    SELECT MaKH, TokenHangDoi
    INTO p_MaKH, p_Token
    FROM (
        SELECT MaKH, TokenHangDoi
        FROM HANGDOIAO
        WHERE MaSK = p_MaSK AND TrangThai = 'Đang chờ'
        ORDER BY ViTriHang ASC
    )
    WHERE ROWNUM = 1;

    UPDATE HANGDOIAO
    SET TrangThai = 'Được vào', ThoiGianHetHan = SYSTIMESTAMP + INTERVAL '5' MINUTE
    WHERE TokenHangDoi = p_Token;

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_MaKH := NULL; p_Token := NULL;
END sp_RaKhoiHangDoiAo;
/


-- ---------------------------------------------------------------
-- SP13: sp_TaoTaiKhoanNguoiDung
-- Tạo tài khoản, gán vai trò mặc định
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_TaoTaiKhoanNguoiDung (
    p_MaND          IN VARCHAR2,
    p_TenTaiKhoan   IN VARCHAR2,
    p_MatKhauHash   IN VARCHAR2,
    p_Email         IN VARCHAR2,
    p_SDT           IN VARCHAR2,
    p_LoaiND        IN VARCHAR2,    -- 'KhachHang' | 'NhanVien'
    p_MaVaiTro      IN VARCHAR2
) AS
BEGIN
    INSERT INTO NGUOIDUNG (MaND, TenTaiKhoan, MatKhauMaHoa, Email, SDT, ThoiGianTao, TrangThaiND)
    VALUES (p_MaND, p_TenTaiKhoan, p_MatKhauHash, p_Email, p_SDT, SYSTIMESTAMP, 'Đang hoạt động');

    -- Gán vai trò
    INSERT INTO CHITIETVAITRO (MaND, MaVaiTro) VALUES (p_MaND, p_MaVaiTro);

    -- Tạo hồ sơ khách hàng nếu là khách hàng
    IF p_LoaiND = 'KhachHang' THEN
        INSERT INTO KHACHHANG (MaKH, HoTenKH, TongChiTieu, CapNhatLanCuoi, MaHangThanhVien, MaND)
        VALUES ('KH_' || p_MaND, p_TenTaiKhoan, 0, SYSTIMESTAMP, 'HANG_DONG', p_MaND);
    END IF;

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20700, 'sp_TaoTaiKhoanNguoiDung thất bại: ' || SQLERRM);
END sp_TaoTaiKhoanNguoiDung;
/


-- ---------------------------------------------------------------
-- SP14: sp_XuatBaoCaoDoanhThu
-- Xuất báo cáo doanh thu theo thời gian thực hoặc từng sự kiện
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_XuatBaoCaoDoanhThu (
    p_MaSK      IN VARCHAR2 DEFAULT NULL,
    p_TuNgay    IN DATE     DEFAULT NULL,
    p_DenNgay   IN DATE     DEFAULT NULL
) AS
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== BÁO CÁO DOANH THU ===');
    DBMS_OUTPUT.PUT_LINE('Tổng doanh thu: ' ||
        fn_TinhDoanhThu(p_MaSK, p_TuNgay, p_DenNgay) || ' VNĐ');

    FOR rec IN (
        SELECT s.TenSK, COUNT(v.MaVe) AS SoVe,
               SUM(v.GiaVe) AS DoanhThu,
               fn_TinhTyLeChuyenDoi(s.MaSK) AS TyLeCD
        FROM SUKIEN s
        LEFT JOIN VE v ON v.MaSK = s.MaSK AND v.TrangThaiVe = 'Đã sử dụng'
        WHERE (p_MaSK IS NULL OR s.MaSK = p_MaSK)
        GROUP BY s.MaSK, s.TenSK
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('SK: ' || rec.TenSK || ' | Vé bán: ' || rec.SoVe
            || ' | DT: ' || NVL(rec.DoanhThu,0) || ' | Tỉ lệ CD: ' || rec.TyLeCD || '%');
    END LOOP;
END sp_XuatBaoCaoDoanhThu;
/


-- ---------------------------------------------------------------
-- SP15: sp_PhanQuyenVaiTro
-- Gán hoặc thu hồi vai trò, ghi audit trail
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_PhanQuyenVaiTro (
    p_MaND_ThucHien  IN VARCHAR2,
    p_MaND_DoiTuong  IN VARCHAR2,
    p_MaVaiTroMoi    IN VARCHAR2,
    p_HanhDong       IN VARCHAR2    -- 'GAN_QUYEN' | 'HUY_QUYEN'
) AS
    v_MaVaiTroCu VARCHAR2(50);
    v_MaNK       VARCHAR2(50);
BEGIN
    -- Lấy vai trò cũ
    BEGIN
        SELECT MaVaiTro INTO v_MaVaiTroCu FROM CHITIETVAITRO WHERE MaND = p_MaND_DoiTuong AND ROWNUM = 1;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_MaVaiTroCu := NULL;
    END;

    IF p_HanhDong = 'GAN_QUYEN' THEN
        MERGE INTO CHITIETVAITRO USING DUAL
        ON (MaND = p_MaND_DoiTuong AND MaVaiTro = p_MaVaiTroMoi)
        WHEN NOT MATCHED THEN INSERT (MaND, MaVaiTro) VALUES (p_MaND_DoiTuong, p_MaVaiTroMoi);
    ELSIF p_HanhDong = 'HUY_QUYEN' THEN
        DELETE FROM CHITIETVAITRO WHERE MaND = p_MaND_DoiTuong AND MaVaiTro = p_MaVaiTroMoi;
    END IF;

    -- Ghi audit trail
    v_MaNK := 'NK_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3');
    INSERT INTO NHATKYQUYENHANTRO (MaNhatKy, HanhDong, MaVaiTroCu, MaVaiTroMoi,
                                    ThoiGianThucHien, MaND_ThucHien, MaND_BiThayDoi)
    VALUES (v_MaNK, p_HanhDong, v_MaVaiTroCu, p_MaVaiTroMoi,
            SYSTIMESTAMP, p_MaND_ThucHien, p_MaND_DoiTuong);

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20800, 'sp_PhanQuyenVaiTro thất bại: ' || SQLERRM);
END sp_PhanQuyenVaiTro;
/

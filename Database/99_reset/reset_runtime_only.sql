-- Reset dữ liệu phát sinh khi demo, giữ lại user/role/sự kiện/khu vực/ghế.
-- Chạy trong Oracle SQL Developer hoặc SQLPlus bằng user/schema của ứng dụng.
-- Nếu bảng nào không tồn tại, script sẽ ghi chú "Bỏ qua" qua DBMS_OUTPUT.

SET SERVEROUTPUT ON;
SET DEFINE OFF;

DECLARE
    PROCEDURE delete_if_exists(p_table_name IN VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
        INTO v_count
        FROM USER_TABLES
        WHERE TABLE_NAME = UPPER(p_table_name);

        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'DELETE FROM ' || p_table_name;
            DBMS_OUTPUT.PUT_LINE('Đã xóa dữ liệu bảng ' || p_table_name);
        ELSE
            DBMS_OUTPUT.PUT_LINE('Bỏ qua bảng không tồn tại: ' || p_table_name);
        END IF;
    END;

    PROCEDURE execute_if_table_exists(p_table_name IN VARCHAR2, p_sql IN VARCHAR2, p_label IN VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
        INTO v_count
        FROM USER_TABLES
        WHERE TABLE_NAME = UPPER(p_table_name);

        IF v_count > 0 THEN
            EXECUTE IMMEDIATE p_sql;
            DBMS_OUTPUT.PUT_LINE(p_label || ': ' || p_table_name);
        ELSE
            DBMS_OUTPUT.PUT_LINE('Bỏ qua bảng không tồn tại: ' || p_table_name);
        END IF;
    END;

    PROCEDURE drop_constraint_if_exists(p_table_name IN VARCHAR2, p_constraint_name IN VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
        INTO v_count
        FROM USER_CONSTRAINTS
        WHERE TABLE_NAME = UPPER(p_table_name)
          AND CONSTRAINT_NAME = UPPER(p_constraint_name);

        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table_name || ' DROP CONSTRAINT ' || p_constraint_name;
            DBMS_OUTPUT.PUT_LINE('Đã xóa constraint cũ: ' || p_constraint_name);
        END IF;
    END;

    PROCEDURE add_constraint_if_table_exists(
        p_table_name IN VARCHAR2,
        p_constraint_name IN VARCHAR2,
        p_condition IN VARCHAR2,
        p_enable_clause IN VARCHAR2 DEFAULT 'ENABLE'
    ) IS
        v_table_count NUMBER;
        v_constraint_count NUMBER;
    BEGIN
        SELECT COUNT(*)
        INTO v_table_count
        FROM USER_TABLES
        WHERE TABLE_NAME = UPPER(p_table_name);

        IF v_table_count = 0 THEN
            DBMS_OUTPUT.PUT_LINE('Bỏ qua constraint vì bảng không tồn tại: ' || p_table_name);
            RETURN;
        END IF;

        SELECT COUNT(*)
        INTO v_constraint_count
        FROM USER_CONSTRAINTS
        WHERE TABLE_NAME = UPPER(p_table_name)
          AND CONSTRAINT_NAME = UPPER(p_constraint_name);

        IF v_constraint_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table_name || ' ADD CONSTRAINT ' || p_constraint_name ||
                              ' CHECK (' || p_condition || ') ' || p_enable_clause;
            DBMS_OUTPUT.PUT_LINE('Đã tạo constraint Unicode: ' || p_constraint_name);
        END IF;
    END;
BEGIN
    drop_constraint_if_exists('DONHANG', 'CHK_DH_TRANGTHAI');
    drop_constraint_if_exists('GIAODICHTHANHTOAN', 'CHK_GD_PHUONGTHUC');
    drop_constraint_if_exists('GIAODICHTHANHTOAN', 'CHK_GD_TRANGTHAI');
    drop_constraint_if_exists('GHENGOI', 'CHK_GHE_TRANGTHAI');
    drop_constraint_if_exists('HANGDOIAO', 'CHK_HDT_TRANGTHAI');
    drop_constraint_if_exists('KHUVUC', 'CHK_KV_TRANGTHAI');
    drop_constraint_if_exists('LICHSUSOATVE', 'CHK_LSSV_KETQUA');
    drop_constraint_if_exists('NGUOIDUNG', 'CHK_ND_GIOITINH');
    drop_constraint_if_exists('NGUOIDUNG', 'CHK_ND_TRANGTHAI');
    drop_constraint_if_exists('NHANVIEN', 'CHK_NV_LOAINV');
    drop_constraint_if_exists('SUKIEN', 'CHK_SK_TRANGTHAI');
    drop_constraint_if_exists('VE', 'CHK_VE_TRANGTHAI');

    delete_if_exists('LICHSUSOATVE');
    delete_if_exists('LICHSUGUI_EMAIL');
    delete_if_exists('GIAODICHTHANHTOAN');
    delete_if_exists('VE');
    delete_if_exists('DONHANG');
    delete_if_exists('HANGDOIAO');
    delete_if_exists('LOG_HANH_VI');

    execute_if_table_exists('GHENGOI', q'[UPDATE GHENGOI SET TrangThaiGhe = UNISTR('\0054\0072\1ED1\006E\0067'), ThoiGianKhoaTam = NULL, MaPhienKhoa = NULL]', 'Đã reset ghế');
    execute_if_table_exists('KHUVUC', 'UPDATE KHUVUC SET SoGheDaBan = 0', 'Đã reset khu vực');
    execute_if_table_exists('KHUVUC', q'[UPDATE KHUVUC SET TrangThai = UNISTR('\0110\0061\006E\0067\0020\0062\00E1\006E') WHERE MaSK = 'SK001']', 'Đã mở bán khu vực mẫu');
    execute_if_table_exists('SUKIEN', 'UPDATE SUKIEN SET SoVeDaBan = 0', 'Đã reset sự kiện');
    execute_if_table_exists('SUKIEN', q'[UPDATE SUKIEN
        SET TrangThaiSK = UNISTR('\0110\0061\006E\0067\0020\006D\1EDF\0020\0062\00E1\006E'),
            ThoiGianBatDau = TO_TIMESTAMP('2026-07-15 19:00:00', 'YYYY-MM-DD HH24:MI:SS'),
            ThoiGianKetThuc = TO_TIMESTAMP('2026-07-15 23:00:00', 'YYYY-MM-DD HH24:MI:SS'),
            ThoiGianMoBan = TO_TIMESTAMP('2026-05-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS'),
            ThoiGianDongBan = TO_TIMESTAMP('2026-07-14 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
            TongSoVe = 65
        WHERE MaSK = 'SK001']', 'Đã reset sự kiện mẫu');
    execute_if_table_exists('NGUOIDUNG', q'[UPDATE NGUOIDUNG
        SET TrangThaiND = UNISTR('\0110\0061\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067')
        WHERE TenTaiKhoan IN ('admin', 'customer', 'staff', 'organizer')]', 'Đã mở khóa user demo');

    add_constraint_if_table_exists('DONHANG', 'CHK_DH_TRANGTHAI', q'[TrangThaiDonHang IN (UNISTR('\0043\0068\1EDD\0020\0074\0068\0061\006E\0068\0020\0074\006F\00E1\006E'), UNISTR('\0110\00E3\0020\0074\0068\0061\006E\0068\0020\0074\006F\00E1\006E'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'), UNISTR('\0048\006F\00E0\006E\0020\0074\0069\1EC1\006E'))]');
    add_constraint_if_table_exists('GIAODICHTHANHTOAN', 'CHK_GD_PHUONGTHUC', q'[PhuongThucTT IN (UNISTR('\0043\0068\0075\0079\1EC3\006E\0020\006B\0068\006F\1EA3\006E'), UNISTR('\0054\0068\1EBB\0020\0074\00ED\006E\0020\0064\1EE5\006E\0067'), UNISTR('\0056\00ED\0020\0111\0069\1EC7\006E\0020\0074\1EED'), UNISTR('\0054\0069\1EC1\006E\0020\006D\1EB7\0074'))]');
    add_constraint_if_table_exists('GIAODICHTHANHTOAN', 'CHK_GD_TRANGTHAI', q'[TrangThaiGD IN (UNISTR('\0054\0068\00E0\006E\0068\0020\0063\00F4\006E\0067'), UNISTR('\0054\0068\1EA5\0074\0020\0062\1EA1\0069'), UNISTR('\0110\0061\006E\0067\0020\0078\1EED\0020\006C\00FD'), UNISTR('\0048\1EBF\0074\0020\0074\0068\1EDD\0069\0020\0067\0069\0061\006E'))]');
    add_constraint_if_table_exists('GHENGOI', 'CHK_GHE_TRANGTHAI', q'[TrangThaiGhe IN (UNISTR('\0054\0072\1ED1\006E\0067'), UNISTR('\0110\0061\006E\0067\0020\0063\0068\1ECD\006E'), UNISTR('\0110\00E3\0020\0062\00E1\006E'), UNISTR('\0042\1EA3\006F\0020\0074\0072\00EC'))]');
    add_constraint_if_table_exists('HANGDOIAO', 'CHK_HDT_TRANGTHAI', q'[TrangThai IN (UNISTR('\0110\0061\006E\0067\0020\0063\0068\1EDD'), UNISTR('\0110\01B0\1EE3\0063\0020\0076\00E0\006F'), UNISTR('\0048\1EBF\0074\0020\0068\1EA1\006E'))]');
    add_constraint_if_table_exists('KHUVUC', 'CHK_KV_TRANGTHAI', q'[TrangThai IN (UNISTR('\0110\0061\006E\0067\0020\0062\00E1\006E'), UNISTR('\0110\00E3\0020\0068\1EBF\0074\0020\0076\00E9'), UNISTR('\0054\1EA1\006D\0020\006B\0068\00F3\0061'))]', 'ENABLE NOVALIDATE');
    add_constraint_if_table_exists('LICHSUSOATVE', 'CHK_LSSV_KETQUA', q'[KetQuaQuet IN (UNISTR('\0048\1EE3\0070\0020\006C\1EC7'), UNISTR('\0056\00E9\0020\0067\0069\1EA3'), UNISTR('\0056\00E9\0020\0111\00E3\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0053\0061\0069\0020\0073\1EF1\0020\006B\0069\1EC7\006E'), UNISTR('\0056\00E9\0020\006B\0068\00F4\006E\0067\0020\0074\00EC\006D\0020\0074\0068\1EA5\0079'))]');
    add_constraint_if_table_exists('NGUOIDUNG', 'CHK_ND_GIOITINH', q'[GioiTinh IN ('Nam', UNISTR('\004E\1EEF'), UNISTR('\004B\0068\00E1\0063'))]', 'ENABLE NOVALIDATE');
    add_constraint_if_table_exists('NGUOIDUNG', 'CHK_ND_TRANGTHAI', q'[TrangThaiND IN (UNISTR('\0110\0061\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'), UNISTR('\004B\0068\00F4\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'), UNISTR('\0042\1ECB\0020\006B\0068\00F3\0061'))]', 'ENABLE NOVALIDATE');
    add_constraint_if_table_exists('NHANVIEN', 'CHK_NV_LOAINV', q'[LoaiNV IN (UNISTR('\0042\0061\006E\0020\0074\1ED5\0020\0063\0068\1EE9\0063'), UNISTR('\004E\0068\00E2\006E\0020\0076\0069\00EA\006E\0020\0073\006F\00E1\0074\0020\0076\00E9'), UNISTR('\0051\0075\1EA3\006E\0020\006C\00FD'))]', 'ENABLE NOVALIDATE');
    add_constraint_if_table_exists('SUKIEN', 'CHK_SK_TRANGTHAI', q'[TrangThaiSK IN (UNISTR('\0043\0068\01B0\0061\0020\006D\1EDF\0020\0062\00E1\006E'), UNISTR('\0110\0061\006E\0067\0020\006D\1EDF\0020\0062\00E1\006E'), UNISTR('\0110\00E3\0020\006B\1EBF\0074\0020\0074\0068\00FA\0063'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'), UNISTR('\0054\1EA1\006D\0020\006E\0067\01B0\006E\0067'))]', 'ENABLE NOVALIDATE');
    add_constraint_if_table_exists('VE', 'CHK_VE_TRANGTHAI', q'[TrangThaiVe IN (UNISTR('\0043\0068\01B0\0061\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0110\00E3\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'))]');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Reset runtime demo hoàn tất.');
END;
/

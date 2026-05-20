-- Reset toàn bộ dữ liệu demo/runtime, giữ nguyên schema/table.
-- Chạy trong Oracle SQL Developer hoặc SQLPlus bằng user/schema của ứng dụng.
-- Dùng DELETE thay vì TRUNCATE để tránh lỗi foreign key.
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
        p_condition IN VARCHAR2
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
                              ' CHECK (' || p_condition || ') ENABLE';
            DBMS_OUTPUT.PUT_LINE('Đã tạo constraint Unicode: ' || p_constraint_name);
        END IF;
    END;
BEGIN
    drop_constraint_if_exists('DIADIEM', 'CHK_DD_TRANGTHAI');
    drop_constraint_if_exists('DONHANG', 'CHK_DH_TRANGTHAI');
    drop_constraint_if_exists('GIAODICHTHANHTOAN', 'CHK_GD_PHUONGTHUC');
    drop_constraint_if_exists('GIAODICHTHANHTOAN', 'CHK_GD_TRANGTHAI');
    drop_constraint_if_exists('GHENGOI', 'CHK_GHE_TRANGTHAI');
    drop_constraint_if_exists('HANGDOIAO', 'CHK_HDT_TRANGTHAI');
    drop_constraint_if_exists('HANGTHANHVIEN', 'CHK_HTV_TENHANG');
    drop_constraint_if_exists('KHUVUC', 'CHK_KV_TRANGTHAI');
    drop_constraint_if_exists('LICHSUSOATVE', 'CHK_LSSV_KETQUA');
    drop_constraint_if_exists('NGUOIDUNG', 'CHK_ND_GIOITINH');
    drop_constraint_if_exists('NGUOIDUNG', 'CHK_ND_TRANGTHAI');
    drop_constraint_if_exists('NHANVIEN', 'CHK_NV_LOAINV');
    drop_constraint_if_exists('SUKIEN', 'CHK_SK_TRANGTHAI');
    drop_constraint_if_exists('VE', 'CHK_VE_TRANGTHAI');

    -- Runtime / child tables trước.
    delete_if_exists('LICHSUSOATVE');
    delete_if_exists('LICHSUGUI_EMAIL');
    delete_if_exists('GIAODICHTHANHTOAN');
    delete_if_exists('VE');
    delete_if_exists('DONHANG');
    delete_if_exists('HANGDOIAO');
    delete_if_exists('LOG_HANH_VI');

    -- Sự kiện / ghế.
    delete_if_exists('GHENGOI');
    delete_if_exists('KHUVUC');
    delete_if_exists('SUKIEN');
    delete_if_exists('PHIEUGIAMGIA');

    -- Người dùng / nhân sự / phân quyền.
    delete_if_exists('KHACHHANG');
    delete_if_exists('NHANVIEN');
    delete_if_exists('CHITIETVAITRO');
    delete_if_exists('CHITIETNHOMCHUCNANG');
    delete_if_exists('CHITIETCHUCNANG');
    -- Bảng audit này có FK tới NGUOIDUNG trong script Foreign.sql, nên xóa trước NGUOIDUNG nếu tồn tại.
    delete_if_exists('NHATKYQUYENHANTRO');
    delete_if_exists('NGUOIDUNG');
    delete_if_exists('VAITRO');
    delete_if_exists('NHOMCHUCNANG');
    delete_if_exists('CHUCNANG');

    -- Master data cuối cùng.
    delete_if_exists('DIADIEM');
    delete_if_exists('LOAISUKIEN');
    delete_if_exists('HANGTHANHVIEN');

    add_constraint_if_table_exists('DIADIEM', 'CHK_DD_TRANGTHAI', q'[TrangThai IN (UNISTR('\0110\0061\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'), UNISTR('\0054\1EA1\006D\0020\006E\0067\01B0\006E\0067'), UNISTR('\004E\0067\1EEB\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'))]');
    add_constraint_if_table_exists('DONHANG', 'CHK_DH_TRANGTHAI', q'[TrangThaiDonHang IN (UNISTR('\0043\0068\1EDD\0020\0074\0068\0061\006E\0068\0020\0074\006F\00E1\006E'), UNISTR('\0110\00E3\0020\0074\0068\0061\006E\0068\0020\0074\006F\00E1\006E'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'), UNISTR('\0048\006F\00E0\006E\0020\0074\0069\1EC1\006E'))]');
    add_constraint_if_table_exists('GIAODICHTHANHTOAN', 'CHK_GD_PHUONGTHUC', q'[PhuongThucTT IN (UNISTR('\0043\0068\0075\0079\1EC3\006E\0020\006B\0068\006F\1EA3\006E'), UNISTR('\0054\0068\1EBB\0020\0074\00ED\006E\0020\0064\1EE5\006E\0067'), UNISTR('\0056\00ED\0020\0111\0069\1EC7\006E\0020\0074\1EED'), UNISTR('\0054\0069\1EC1\006E\0020\006D\1EB7\0074'))]');
    add_constraint_if_table_exists('GIAODICHTHANHTOAN', 'CHK_GD_TRANGTHAI', q'[TrangThaiGD IN (UNISTR('\0054\0068\00E0\006E\0068\0020\0063\00F4\006E\0067'), UNISTR('\0054\0068\1EA5\0074\0020\0062\1EA1\0069'), UNISTR('\0110\0061\006E\0067\0020\0078\1EED\0020\006C\00FD'), UNISTR('\0048\1EBF\0074\0020\0074\0068\1EDD\0069\0020\0067\0069\0061\006E'))]');
    add_constraint_if_table_exists('GHENGOI', 'CHK_GHE_TRANGTHAI', q'[TrangThaiGhe IN (UNISTR('\0054\0072\1ED1\006E\0067'), UNISTR('\0110\0061\006E\0067\0020\0063\0068\1ECD\006E'), UNISTR('\0110\00E3\0020\0062\00E1\006E'), UNISTR('\0042\1EA3\006F\0020\0074\0072\00EC'))]');
    add_constraint_if_table_exists('HANGDOIAO', 'CHK_HDT_TRANGTHAI', q'[TrangThai IN (UNISTR('\0110\0061\006E\0067\0020\0063\0068\1EDD'), UNISTR('\0110\01B0\1EE3\0063\0020\0076\00E0\006F'), UNISTR('\0048\1EBF\0074\0020\0068\1EA1\006E'))]');
    add_constraint_if_table_exists('HANGTHANHVIEN', 'CHK_HTV_TENHANG', q'[TenHangThanhVien IN (UNISTR('\004B\0068\00F4\006E\0067\0020\0063\00F3'), UNISTR('\0110\1ED3\006E\0067'), UNISTR('\0042\1EA1\0063'), UNISTR('\0056\00E0\006E\0067'), UNISTR('\004B\0069\006D\0020\0063\01B0\01A1\006E\0067'))]');
    add_constraint_if_table_exists('KHUVUC', 'CHK_KV_TRANGTHAI', q'[TrangThai IN (UNISTR('\0110\0061\006E\0067\0020\0062\00E1\006E'), UNISTR('\0110\00E3\0020\0068\1EBF\0074\0020\0076\00E9'), UNISTR('\0054\1EA1\006D\0020\006B\0068\00F3\0061'))]');
    add_constraint_if_table_exists('LICHSUSOATVE', 'CHK_LSSV_KETQUA', q'[KetQuaQuet IN (UNISTR('\0048\1EE3\0070\0020\006C\1EC7'), UNISTR('\0056\00E9\0020\0067\0069\1EA3'), UNISTR('\0056\00E9\0020\0111\00E3\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0053\0061\0069\0020\0073\1EF1\0020\006B\0069\1EC7\006E'), UNISTR('\0056\00E9\0020\006B\0068\00F4\006E\0067\0020\0074\00EC\006D\0020\0074\0068\1EA5\0079'))]');
    add_constraint_if_table_exists('NGUOIDUNG', 'CHK_ND_GIOITINH', q'[GioiTinh IN ('Nam', UNISTR('\004E\1EEF'), UNISTR('\004B\0068\00E1\0063'))]');
    add_constraint_if_table_exists('NGUOIDUNG', 'CHK_ND_TRANGTHAI', q'[TrangThaiND IN (UNISTR('\0110\0061\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'), UNISTR('\004B\0068\00F4\006E\0067\0020\0068\006F\1EA1\0074\0020\0111\1ED9\006E\0067'), UNISTR('\0042\1ECB\0020\006B\0068\00F3\0061'))]');
    add_constraint_if_table_exists('NHANVIEN', 'CHK_NV_LOAINV', q'[LoaiNV IN (UNISTR('\0042\0061\006E\0020\0074\1ED5\0020\0063\0068\1EE9\0063'), UNISTR('\004E\0068\00E2\006E\0020\0076\0069\00EA\006E\0020\0073\006F\00E1\0074\0020\0076\00E9'), UNISTR('\0051\0075\1EA3\006E\0020\006C\00FD'))]');
    add_constraint_if_table_exists('SUKIEN', 'CHK_SK_TRANGTHAI', q'[TrangThaiSK IN (UNISTR('\0043\0068\01B0\0061\0020\006D\1EDF\0020\0062\00E1\006E'), UNISTR('\0110\0061\006E\0067\0020\006D\1EDF\0020\0062\00E1\006E'), UNISTR('\0110\00E3\0020\006B\1EBF\0074\0020\0074\0068\00FA\0063'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'), UNISTR('\0054\1EA1\006D\0020\006E\0067\01B0\006E\0067'))]');
    add_constraint_if_table_exists('VE', 'CHK_VE_TRANGTHAI', q'[TrangThaiVe IN (UNISTR('\0043\0068\01B0\0061\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0110\00E3\0020\0073\1EED\0020\0064\1EE5\006E\0067'), UNISTR('\0110\00E3\0020\0068\1EE7\0079'))]');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Reset toàn bộ dữ liệu hoàn tất.');
END;
/

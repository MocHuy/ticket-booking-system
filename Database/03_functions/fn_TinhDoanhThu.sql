CREATE OR REPLACE FUNCTION fn_TinhDoanhThu (
    p_MaSK          IN VARCHAR2 DEFAULT NULL,
    p_TuNgay        IN DATE     DEFAULT NULL,
    p_DenNgay       IN DATE     DEFAULT NULL
) RETURN NUMBER IS
    v_DoanhThu NUMBER(18,2);
BEGIN
    SELECT NVL(SUM(g.SoTienThanhToan), 0)
    INTO v_DoanhThu
    FROM GIAODICHTHANHTOAN g
    JOIN DONHANG d ON g.MaDonHang = d.MaDonHang
    JOIN VE v ON v.MaDonHang = d.MaDonHang
    WHERE g.TrangThaiGD = 'Thành công'
      AND (p_MaSK IS NULL OR v.MaSK = p_MaSK)
      AND (p_TuNgay IS NULL OR TRUNC(g.ThoiGianThucHien) >= p_TuNgay)
      AND (p_DenNgay IS NULL OR TRUNC(g.ThoiGianThucHien) <= p_DenNgay);

    RETURN v_DoanhThu;
END fn_TinhDoanhThu;
/
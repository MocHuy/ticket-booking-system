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

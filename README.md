# Hệ thống Quản lý Sự kiện & Bán vé trực tuyến

**Mã dự án:** ITPJ2602 | **Nhóm:** SPRING | **Lớp:** IS208.Q21  
**Khách hàng:** Startup Dề Dê

---

## Cấu trúc dự án

```
he-thong-ban-ve/
├── Database/
│   ├── 01_table/
│   │   ├── SuKien_DiaDiem/          # DiaDiem, LoaiSuKien, SuKien, KhuVuc, GheNgoi
│   │   ├── NhanSu_KhachHang/        # NguoiDung, HangThanhVien, KhachHang, NhanVien
│   │   ├── PhanQuyen_BaoMat/        # VaiTro, NhomChucNang, ChucNang + bảng chi tiết (tái sử dụng)
│   │   ├── BanVe_ThanhToan/         # DonHang, Ve, GiaoDichThanhToan, PhieuGiamGia
│   │   └── KiemSoat_HangDoi/        # LichSuSoatVe, HangDoiAo, NhatKyThayDoiQuyen
│   ├── 02_constraint/
│   │   ├── Foreign.sql              # Toàn bộ khóa ngoại
│   │   └── RangBuocMienGiaTri.sql   # Check constraints
│   ├── 03_functions/
│   │   └── TatCaFunctions.sql       # 11 Functions
│   ├── 04_procedures/
│   │   └── TatCaProcedures.sql      # 15 Stored Procedures
│   ├── 05_triggers/
│   │   └── TatCaTriggers.sql        # 10 Triggers
│   ├── 06_types/
│   │   └── t_chi_tiet_ve.sql        # Custom Oracle Types
│   └── 07_data/
│       ├── su_kien_data/            # Dữ liệu mẫu sự kiện
│       ├── nguoi_dung_data/         # Dữ liệu mẫu người dùng
│       ├── ban_ve_data/             # Dữ liệu mẫu vé & đơn hàng
│       └── thanh_toan_data/         # Dữ liệu mẫu giao dịch
└── src/
    └── main/java/com/ticketsys/
        ├── config/KetNoiCSDL.java   # Kết nối CSDL (tái sử dụng)
        ├── model/                   # NguoiDung, Ve, ...
        ├── dao/                     # NguoiDungDAO, VeDAO, ...
        ├── controller/              # DangNhapController, ...
        ├── util/TienIchMatKhau.java # BCrypt util (tái sử dụng)
        └── view/                    # Giao diện (DangNhap, DangKy, SuKien, Ve)
```

---

## Thành phần Database đề xuất

### Triggers (10)
| Tên | Sự kiện | Mục đích |
|-----|---------|----------|
| TRG_KiemTraOverbooking | BEFORE INSERT VE | Chặn tuyệt đối tình trạng overbooking |
| TRG_ChanVeTrungMaQR | BEFORE INSERT VE | Đảm bảo không có 2 vé cùng mã QR |
| TRG_CapNhatTrangThaiGheSauBan | AFTER INSERT VE | Cập nhật ghế → "Đã bán", đếm số ghế |
| TRG_GiaiPhongGheSauHuyVe | AFTER UPDATE VE | Trả ghế về "Trống" khi vé bị hủy |
| TRG_TichLuyChiTieuKhachHang | AFTER INSERT/UPDATE GIAODICH | Tích lũy chi tiêu (tái sử dụng) |
| TRG_TuDongThangHangThanhVien | BEFORE UPDATE KHACHHANG | Thăng hạng thành viên (tái sử dụng) |
| TRG_ChanThanhToanDonHangHetHan | BEFORE INSERT GIAODICH | Chặn thanh toán đơn hết hạn |
| TRG_GhiNhatKyThayDoiQuyen | AFTER INSERT/DELETE CHITIETVAITRO | Audit trail bảo mật |
| TRG_CanhBaoSuKienTaiCao | AFTER UPDATE SUKIEN | Cảnh báo admin khi nhiều SK mở bán |
| TRG_PhatVeSauThanhToanThanhCong | AFTER UPDATE GIAODICH | Phát vé khi thanh toán OK |

### Stored Procedures (15)
`sp_TaoSuKien` · `sp_KhoiTaoSoDoGhe` · `sp_KhoaGheTamThoi` · `sp_DatVe` ·
`sp_XuLyThanhToan` · `sp_ThuLaiThanhToan` · `sp_HuyDonHang` ·
`sp_GiaiPhongGheHetHan` · `sp_XacThucVeTaiCong` · `sp_DongBoLichSuSoatVeOffline` ·
`sp_VaoHangDoiAo` · `sp_RaKhoiHangDoiAo` · `sp_TaoTaiKhoanNguoiDung` ·
`sp_XuatBaoCaoDoanhThu` · `sp_PhanQuyenVaiTro`

### Functions (11)
`fn_LayTrangThaiGhe` · `fn_DemGheConTrong` · `fn_TinhGiaVe` · `fn_SinhMaQRDuyNhat` ·
`fn_XacThucMaQR` · `fn_KiemTraOverbooking` · `fn_KiemTraGioVang` ·
`fn_TinhDoanhThu` · `fn_TinhTyLeChuyenDoi` · `fn_MaHoaDuLieuNhayCam` · `fn_KiemTraGioiHanBot`

---

## Tái sử dụng từ workspace-management-system

| Thành phần | Tái sử dụng |
|------------|-------------|
| `PhanQuyen_BaoMat/` (6 bảng) | 100% nguyên xi |
| `TRG_TichLuyChiTieuKhachHang` | 100% nguyên xi |
| `TRG_TuDongThangHangThanhVien` | 100% nguyên xi |
| `KetNoiCSDL.java` | Đổi tên + package |
| `TienIchMatKhau.java` | Đổi tên + package |
| Mô hình `HangThanhVien` | Tái sử dụng hoàn toàn |
| Cấu trúc `pom.xml` | Điều chỉnh groupId |

---

## Cài đặt

1. Copy `.env.example` → `.env`, điền thông tin Oracle DB
2. Chạy các file SQL theo thứ tự: `01_table` → `02_constraint` → `06_types` → `03_functions` → `04_procedures` → `05_triggers` → `07_data`
3. Build: `mvn clean install`

## Yêu cầu kỹ thuật
- Oracle Database 19c+
- Java 17+
- Maven 3.8+

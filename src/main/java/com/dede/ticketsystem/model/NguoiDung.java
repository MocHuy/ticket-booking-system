package com.dede.ticketsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "NGUOIDUNG")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NguoiDung {

    @Id
    @Column(name = "MaND", length = 50)
    private String maND;

    @Column(name = "TenTaiKhoan", length = 50, unique = true)
    private String tenTaiKhoan;

    @Column(name = "MatKhauMaHoa", length = 255)
    private String matKhauMaHoa;

    @Column(name = "AnhDaiDien", length = 500)
    private String anhDaiDien;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh;

    @Column(name = "Email", length = 100, unique = true)
    private String email;

    @Column(name = "SDT", length = 20)
    private String sdt;

    @Column(name = "NgaySinh")
    private Date ngaySinh;

    @Column(name = "ThoiGianTao")
    private Timestamp thoiGianTao;

    @Column(name = "CapNhatLanCuoi")
    private Timestamp capNhatLanCuoi;

    @Column(name = "LanCuoiDangNhap")
    private Timestamp lanCuoiDangNhap;

    @Column(name = "TrangThaiND", length = 50)
    private String trangThaiND;   // 'Hoat_dong' | 'Bi_khoa' | 'Cho_xac_nhan'

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietVaiTro> chiTietVaiTros;
    
    public boolean isAdmin() {
    if (this.chiTietVaiTros == null) {
        return false;
    }
 
    return this.chiTietVaiTros.stream()
            .anyMatch(ct -> ct.getVaiTro().getMaVaiTro().equals("ADMIN")); 
}
}
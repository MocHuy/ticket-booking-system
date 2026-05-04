package com.dede.ticketsystem.model;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiKhoanDTO {

    private String maND;
    private String tenTaiKhoan;
    private String matKhau;          // plain-text, se ma hoa trong service
    private String email;
    private String sdt;
    private String gioiTinh;
    private String trangThaiND;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date ngaySinh;

    private List<String> danhSachVaiTro;  // danh sach MaVaiTro duoc chon
}
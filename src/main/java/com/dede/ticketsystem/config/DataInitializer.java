package com.dede.ticketsystem.config;

import com.dede.ticketsystem.model.ChiTietVaiTro;
import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.model.VaiTro;
import com.dede.ticketsystem.repository.NguoiDungRepository;
import com.dede.ticketsystem.repository.VaiTroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        List<VaiTro> defaultRoles = Arrays.asList(
            VaiTro.builder().maVaiTro("ADMIN").tenVaiTro("ADMIN").moTa("Quản trị viên hệ thống").build(),
            VaiTro.builder().maVaiTro("CUSTOMER").tenVaiTro("CUSTOMER").moTa("Khách hàng").build(),
            VaiTro.builder().maVaiTro("STAFF").tenVaiTro("STAFF").moTa("Nhân viên").build(),
            VaiTro.builder().maVaiTro("ORGANIZER").tenVaiTro("ORGANIZER").moTa("Ban tổ chức").build()
        );

        for (VaiTro r : defaultRoles) {
            if (!vaiTroRepository.existsById(r.getMaVaiTro())) {
                vaiTroRepository.save(r);
            }
        }

        if (!nguoiDungRepository.existsById("ND001")) {

            VaiTro adminRole = vaiTroRepository.findById("ADMIN").orElse(null);

            if (adminRole != null) {

                NguoiDung admin = NguoiDung.builder()
                        .maND("ND001")
                        .tenTaiKhoan("admin")
                        .matKhauMaHoa(passwordEncoder.encode("123456"))
                        .trangThaiND("Đang hoạt động")
                        .thoiGianTao(new Timestamp(System.currentTimeMillis()))
                        .chiTietVaiTros(new ArrayList<>())
                        .build();

                ChiTietVaiTro chiTiet = new ChiTietVaiTro();
                chiTiet.setNguoiDung(admin);
                chiTiet.setVaiTro(adminRole);
                chiTiet.setMaND(admin.getMaND());
                chiTiet.setMaVaiTro("ADMIN");

                admin.getChiTietVaiTros().add(chiTiet);

                nguoiDungRepository.save(admin);
                System.out.println("KHỞI TẠO TÀI KHOẢN ADMIN THÀNH CÔNG");
            }
        }
    }
}
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

        if (vaiTroRepository.count() == 0) {
            VaiTro vaiTroAdmin = VaiTro.builder()
                    .maVaiTro("VT01")
                    .tenVaiTro("ADMIN")
                    .moTa("Quản trị viên hệ thống")
                    .build();
            vaiTroRepository.save(vaiTroAdmin);
        }

        if (nguoiDungRepository.count() == 0) {

            VaiTro adminRole = vaiTroRepository.findById("VT01").orElse(null);

            if (adminRole != null) {

                NguoiDung admin = NguoiDung.builder()
                        .maND("ND001")
                        .tenTaiKhoan("admin")
                        .matKhauMaHoa(passwordEncoder.encode("123456"))
                        .trangThaiND("Hoat_dong")
                        .thoiGianTao(new Timestamp(System.currentTimeMillis()))
                        .chiTietVaiTros(new ArrayList<>())
                        .build();

                ChiTietVaiTro chiTiet = new ChiTietVaiTro();
                chiTiet.setNguoiDung(admin);
                chiTiet.setVaiTro(adminRole);
                chiTiet.setMaND(admin.getMaND());
                chiTiet.setMaVaiTro("VT01");

                admin.getChiTietVaiTros().add(chiTiet);

                nguoiDungRepository.save(admin);
                System.out.println("KHỞI TẠO TÀI KHOẢN ADMIN THÀNH CÔNG");
            }
        }
    }
}
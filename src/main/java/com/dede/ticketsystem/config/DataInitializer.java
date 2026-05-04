package com.dede.ticketsystem.config;

import com.dede.ticketsystem.model.NguoiDung;
import com.dede.ticketsystem.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (nguoiDungRepository.count() == 0) {
            NguoiDung admin = new NguoiDung();
            admin.setMaND("ND001");
            admin.setTenTaiKhoan("admin");
            admin.setMatKhauMaHoa(passwordEncoder.encode("123456"));
            admin.setHoTen("Quản trị viên");
            admin.setMaVT("VT01");
            admin.setTrangThaiND("Hoạt động");

            nguoiDungRepository.save(admin);
        }
    }
}

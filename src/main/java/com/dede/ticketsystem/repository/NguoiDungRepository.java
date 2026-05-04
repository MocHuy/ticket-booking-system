package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, String> {
    Optional<NguoiDung> findByTenTaiKhoan(String tenTaiKhoan);
}

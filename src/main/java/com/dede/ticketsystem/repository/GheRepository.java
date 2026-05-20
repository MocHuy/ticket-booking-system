package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.Ghe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface GheRepository extends JpaRepository<Ghe, String> {
    List<Ghe> findByMaSK(String maSK);

    List<Ghe> findByMaKhuVuc(String maKhuVuc);

    @Query("SELECT g FROM Ghe g WHERE g.trangThai = :trangThai AND g.thoiGianGiu < :thoiGian")
    List<Ghe> findExpiredSeats(@org.springframework.data.repository.query.Param("trangThai") String trangThai,
            @org.springframework.data.repository.query.Param("thoiGian") Timestamp thoiGian);
}

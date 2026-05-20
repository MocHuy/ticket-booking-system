package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, String> {

    List<DonHang> findByTrangThaiDonHangIgnoreCase(String trangThai);

    @Query("SELECT d FROM DonHang d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "  LOWER(d.soDonHang) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(d.maKH) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:trangThai IS NULL OR :trangThai = '' OR d.trangThaiDonHang = :trangThai) " +
           "ORDER BY d.thoiGianDat DESC")
    List<DonHang> search(@Param("keyword") String keyword,
                         @Param("trangThai") String trangThai);
}

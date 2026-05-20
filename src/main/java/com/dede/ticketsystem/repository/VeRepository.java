package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.Ve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VeRepository extends JpaRepository<Ve, String> {

    @Query("SELECT v FROM Ve v WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "  LOWER(v.maVe) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(v.maQR) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(v.maSK) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:trangThai IS NULL OR :trangThai = '' OR v.trangThaiVe = :trangThai) " +
           "ORDER BY v.thoiGianPhat DESC")
    List<Ve> search(@Param("keyword") String keyword, @Param("trangThai") String trangThai);

    boolean existsByMaQR(String maQR);

    @Query("SELECT v FROM Ve v WHERE v.maSK = :maSK AND v.maDonHang = :maDonHang")
    List<Ve> findAvailableTickets(@Param("maSK") String maSK, @Param("maDonHang") String maDonHang, org.springframework.data.domain.Pageable pageable);

    Ve findByMaGhe(String maGhe);

    List<Ve> findByMaSK(String maSK);
}

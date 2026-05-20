package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.GiaoDichThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GiaoDichThanhToanRepository extends JpaRepository<GiaoDichThanhToan, String> {
}

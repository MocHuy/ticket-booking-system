package com.dede.ticketsystem.repository;

import com.dede.ticketsystem.model.ChiTietVaiTro;
import com.dede.ticketsystem.model.ChiTietVaiTroID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietVaiTroRepository extends JpaRepository<ChiTietVaiTro, ChiTietVaiTroID> {

    List<ChiTietVaiTro> findByMaND(String maND);

    void deleteByMaND(String maND);
}
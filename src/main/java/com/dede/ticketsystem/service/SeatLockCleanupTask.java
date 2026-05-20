package com.dede.ticketsystem.service;

import com.dede.ticketsystem.model.Ghe;
import com.dede.ticketsystem.repository.GheRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Component
public class SeatLockCleanupTask {

    private final GheRepository gheRepository;

    public SeatLockCleanupTask(GheRepository gheRepository) {
        this.gheRepository = gheRepository;
    }

    // Chạy mỗi 60 giây (60000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void unlockExpiredSeats() {
        // 10 phút trước
        Timestamp tenMinsAgo = new Timestamp(System.currentTimeMillis() - (10 * 60 * 1000));
        
        // Tìm các ghế Đang giữ và thời gian giữ < 10 phút trước (tức là đã giữ hơn 10 phút)
        List<Ghe> expiredSeats = gheRepository.findExpiredSeats("Đang giữ", tenMinsAgo);
        
        if (!expiredSeats.isEmpty()) {
            for (Ghe ghe : expiredSeats) {
                ghe.setTrangThai("Trống");
                ghe.setThoiGianGiu(null);
                ghe.setMaKHDangGiu(null);
            }
            gheRepository.saveAll(expiredSeats);
            System.out.println("[SeatLockCleanupTask] Đã nhả " + expiredSeats.size() + " ghế hết hạn giữ chỗ.");
        }
    }
}

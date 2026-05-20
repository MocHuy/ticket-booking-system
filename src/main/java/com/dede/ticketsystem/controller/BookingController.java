package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/lock-seats")
    public ResponseEntity<?> lockSeats(@RequestBody Map<String, Object> payload) {
        try {
            List<String> maGheList = (List<String>) payload.get("maGheList");
            // Demo: hardcode maKH for now, or get from session
            String maKH = "KH001"; 
            
            bookingService.lockSeats(maGheList, maKH);
            
            String seatsParam = String.join(",", maGheList);
            return ResponseEntity.ok(Map.of("message", "Đã khóa ghế thành công", "redirect", "/thanh-toan?seats=" + seatsParam));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> payload) {
        try {
            String seats = (String) payload.get("seats");
            boolean success = (Boolean) payload.get("success");
            List<String> maGheList = List.of(seats.split(","));
            String maKH = "KH001"; // Demo user
            
            bookingService.processCheckout(maGheList, success, maKH);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Thanh toán THÀNH CÔNG! Đơn hàng và Vé đã được tạo.", "redirect", "/"));
            } else {
                return ResponseEntity.ok(Map.of("message", "Thanh toán THẤT BẠI! Đã hủy đơn hàng và giải phóng ghế.", "redirect", "/"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi xử lý thanh toán: " + e.getMessage()));
        }
    }
}

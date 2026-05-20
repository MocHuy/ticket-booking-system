package com.dede.ticketsystem.controller;

import com.dede.ticketsystem.service.BookingService;
import com.dede.ticketsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    private SessionService sessionService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/lock-seats")
    public ResponseEntity<?> lockSeats(@RequestBody Map<String, Object> payload) {
        try {
            if (!sessionService.isLoggedIn()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Bạn cần đăng nhập để đặt vé", "redirect", "/dang-nhap"));
            }
            if (!sessionService.hasRole("CUSTOMER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập chức năng này"));
            }

            String maKH = sessionService.getCurrentMaKH();
            if (maKH == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Tài khoản của bạn chưa có hồ sơ Khách hàng. Vui lòng liên hệ Admin.", "redirect", "/dang-nhap"));
            }

            List<String> maGheList = (List<String>) payload.get("maGheList");
            String maSK = (String) payload.get("maSK");

            if (maGheList == null || maGheList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách ghế chọn không được trống!"));
            }

            // Gọi logic giữ ghế
            String orderId = bookingService.lockSeats(maGheList, maSK, maKH);
            
            return ResponseEntity.ok(Map.of("message", "Đã giữ ghế thành công", "redirect", "/thanh-toan?orderId=" + orderId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> payload) {
        try {
            if (!sessionService.isLoggedIn()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Bạn cần đăng nhập để thực hiện thanh toán", "redirect", "/dang-nhap"));
            }
            if (!sessionService.hasRole("CUSTOMER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền truy cập chức năng này"));
            }

            String maKH = sessionService.getCurrentMaKH();
            if (maKH == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Tài khoản của bạn chưa có hồ sơ Khách hàng", "redirect", "/dang-nhap"));
            }

            String orderId = (String) payload.get("orderId");
            if (orderId == null) {
                // Hỗ trợ fallback nếu client gửi param cũ
                orderId = (String) payload.get("seats");
            }
            
            Object successObj = payload.get("success");
            boolean success = false;
            if (successObj instanceof Boolean) {
                success = (Boolean) successObj;
            } else if (successObj instanceof String) {
                success = Boolean.parseBoolean((String) successObj);
            }

            String simulateResult = success ? "Thành công" : "Thất bại";
            String paymentMethod = (String) payload.get("paymentMethod");
            if (paymentMethod == null) {
                paymentMethod = "Chuyển khoản ngân hàng";
            }

            bookingService.processCheckout(orderId, maKH, paymentMethod, simulateResult);
            
            return ResponseEntity.ok(Map.of("message", "Thanh toán thành công! Vé của bạn đã sẵn sàng.", "redirect", "/"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel-order")
    public ResponseEntity<?> cancelOrder(@RequestBody Map<String, Object> payload) {
        try {
            if (!sessionService.isLoggedIn()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Bạn cần đăng nhập để thực hiện", "redirect", "/dang-nhap"));
            }

            String maKH = sessionService.getCurrentMaKH();
            if (maKH == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Tài khoản của bạn chưa có hồ sơ Khách hàng", "redirect", "/dang-nhap"));
            }

            String orderId = (String) payload.get("orderId");
            if (orderId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu mã đơn hàng!"));
            }

            bookingService.cancelOrder(orderId, maKH);
            return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng và giải phóng ghế thành công.", "redirect", "/"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

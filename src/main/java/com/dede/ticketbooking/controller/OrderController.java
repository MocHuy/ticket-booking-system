package com.dede.ticketbooking.controller;

import com.dede.ticketbooking.dto.ApiResponse;
import com.dede.ticketbooking.dto.BookingDTO.*;
import com.dede.ticketbooking.service.BookingService;
import com.dede.ticketbooking.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        Long userId = (Long) auth.getCredentials();
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        OrderResponse order = bookingService.createOrder(request, userId, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok("Đơn hàng đã được tạo. Vui lòng thanh toán trong 10 phút.", order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getOrder(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId) {
        bookingService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Đơn hàng đã được hủy", null));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/pay/{paymentId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.retryPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{orderId}/tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getOrderTickets(@PathVariable Long orderId) {
        List<TicketResponse> tickets = bookingService.generateTickets(orderId);
        return ResponseEntity.ok(ApiResponse.ok(tickets));
    }
}

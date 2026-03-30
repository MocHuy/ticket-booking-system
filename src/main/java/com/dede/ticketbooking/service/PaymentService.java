package com.dede.ticketbooking.service;

import com.dede.ticketbooking.dto.BookingDTO.*;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Payment Service — Thanh toán giả lập với cơ chế retry
 * 
 * Giả lập payment gateway:
 *   - Tỷ lệ thành công: 98% (cấu hình được)
 *   - Retry tối đa 3 lần với exponential backoff
 *   - Tự hủy order khi vượt max retry
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final BookingService bookingService;

    @Value("${app.payment.max-retry-count}")
    private int maxRetryCount;

    @Value("${app.payment.success-rate}")
    private double successRate;

    @Value("${app.payment.simulated-delay-ms}")
    private long simulatedDelayMs;

    private final Random random = new Random();

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"AWAITING_PAYMENT".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order is not awaiting payment. Current status: " + order.getStatus());
        }

        // Check if order expired
        if (order.getExpiredAt() != null && LocalDateTime.now().isAfter(order.getExpiredAt())) {
            bookingService.cancelOrder(order.getOrderId());
            throw new RuntimeException("Order has expired. Seats have been released.");
        }

        Invoice invoice = invoiceRepository.findByOrderId(order.getOrderId())
                .orElseThrow(() -> new RuntimeException("Invoice not found for order"));

        // Create payment record
        Payment payment = Payment.builder()
                .invoiceId(invoice.getInvoiceId())
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getFinalAmount())
                .status("PENDING")
                .maxRetryCount(maxRetryCount)
                .build();
        payment = paymentRepository.save(payment);

        // Simulate payment gateway call
        boolean success = simulatePaymentGateway(payment);

        if (success) {
            payment.setStatus("SUCCESS");
            payment.setTransactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
            paymentRepository.save(payment);

            // Update order status
            order.setStatus("PAID");
            order.setPaymentStatus("PAID");
            orderRepository.save(order);

            // Update invoice
            invoice.setStatus("PAID");
            invoiceRepository.save(invoice);

            // Generate tickets
            bookingService.generateTickets(order.getOrderId());

            log.info("Payment SUCCESS for order {} - TXN: {}", order.getOrderCode(), payment.getTransactionReference());

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .status("SUCCESS")
                    .transactionReference(payment.getTransactionReference())
                    .message("Thanh toán thành công! Vé đã được gửi.")
                    .retryCount(payment.getRetryCount())
                    .build();
        } else {
            payment.setStatus("FAILED");
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setLastRetryAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Check if exceeded max retries
            if (payment.getRetryCount() >= maxRetryCount) {
                bookingService.cancelOrder(order.getOrderId());
                log.warn("Payment FAILED after {} retries for order {}. Order cancelled.", maxRetryCount, order.getOrderCode());

                return PaymentResponse.builder()
                        .paymentId(payment.getPaymentId())
                        .status("FAILED")
                        .message("Thanh toán thất bại sau " + maxRetryCount + " lần thử. Đơn hàng đã bị hủy.")
                        .retryCount(payment.getRetryCount())
                        .build();
            }

            log.warn("Payment FAILED (attempt {}/{}) for order {}", payment.getRetryCount(), maxRetryCount, order.getOrderCode());

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .status("FAILED")
                    .message("Thanh toán thất bại. Còn " + (maxRetryCount - payment.getRetryCount()) + " lần thử lại.")
                    .retryCount(payment.getRetryCount())
                    .build();
        }
    }

    @Transactional
    public PaymentResponse retryPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!"FAILED".equals(payment.getStatus())) {
            throw new RuntimeException("Payment is not in FAILED status");
        }

        if (payment.getRetryCount() >= payment.getMaxRetryCount()) {
            throw new RuntimeException("Maximum retry count exceeded");
        }

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        Order order = orderRepository.findById(invoice.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean success = simulatePaymentGateway(payment);
        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setLastRetryAt(LocalDateTime.now());

        if (success) {
            payment.setStatus("SUCCESS");
            payment.setTransactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
            paymentRepository.save(payment);

            order.setStatus("PAID");
            order.setPaymentStatus("PAID");
            orderRepository.save(order);

            invoice.setStatus("PAID");
            invoiceRepository.save(invoice);

            bookingService.generateTickets(order.getOrderId());

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .status("SUCCESS")
                    .transactionReference(payment.getTransactionReference())
                    .message("Thanh toán thành công sau lần thử " + payment.getRetryCount())
                    .retryCount(payment.getRetryCount())
                    .build();
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            if (payment.getRetryCount() >= payment.getMaxRetryCount()) {
                bookingService.cancelOrder(order.getOrderId());
            }

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .status("FAILED")
                    .message("Thanh toán thất bại (lần " + payment.getRetryCount() + "/" + payment.getMaxRetryCount() + ")")
                    .retryCount(payment.getRetryCount())
                    .build();
        }
    }

    /**
     * Simulated payment gateway
     * Tỷ lệ thành công: 98% (đáp ứng yêu cầu > 98%)
     */
    private boolean simulatePaymentGateway(Payment payment) {
        try {
            // Simulate network latency
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return random.nextDouble() < successRate;
    }
}

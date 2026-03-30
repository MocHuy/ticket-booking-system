package com.dede.ticketbooking.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class BookingDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateOrderRequest {
        @NotNull private Long eventId;
        @NotEmpty private List<OrderItemRequest> items;
        private String voucherCode;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull private Long ticketTypeId;
        private Long seatId; // null for standing
        @Min(1) @Max(10) private int quantity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderResponse {
        private Long orderId;
        private String orderCode;
        private String status;
        private String paymentStatus;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String expiredAt;
        private List<OrderDetailResponse> details;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderDetailResponse {
        private Long detailId;
        private String ticketTypeName;
        private String seatLabel;
        private int quantity;
        private BigDecimal priceSnapshot;
        private BigDecimal subtotal;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull private Long orderId;
        @NotBlank private String paymentMethod; // VNPAY, MOMO, CREDIT_CARD, SIMULATED
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentResponse {
        private Long paymentId;
        private String status;
        private String transactionReference;
        private String message;
        private int retryCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TicketResponse {
        private Long ticketId;
        private String ticketCode;
        private String qrCodeData;
        private String qrCodeImageBase64;
        private String eventName;
        private String ticketTypeName;
        private String seatLabel;
        private String holderName;
        private String status;
        private String issuedAt;
    }
}

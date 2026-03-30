package com.dede.ticketbooking.service;

import com.dede.ticketbooking.dto.ScanDTO.*;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket Scanning Service — Soát vé < 5 giây
 * 
 * Flow:
 *   1. Nhận ticket_code từ QR scan
 *   2. Verify HMAC signature (offline capable)
 *   3. Lookup ticket in DB
 *   4. Check status (VALID → USED)
 *   5. Log scan result
 * 
 * Hỗ trợ offline: app lưu kết quả scan local, sync về server sau
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final TicketRepository ticketRepository;
    private final TicketScanRepository ticketScanRepository;
    private final QRCodeService qrCodeService;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public ScanResponse scanTicket(ScanRequest request) {
        long startTime = System.currentTimeMillis();
        String ticketCode = request.getTicketCode();

        // If QR data includes HMAC signature, extract ticket code
        if (ticketCode.contains("|")) {
            if (!qrCodeService.verifyQRCodeData(ticketCode)) {
                return buildFailResponse("INVALID", "QR code chữ ký không hợp lệ - có thể là vé giả!", 
                        ticketCode, startTime);
            }
            ticketCode = qrCodeService.extractTicketCode(ticketCode);
        }

        // Lookup ticket
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode).orElse(null);

        if (ticket == null) {
            logScan(request, null, "INVALID");
            return buildFailResponse("INVALID", "Mã vé không tồn tại trong hệ thống", ticketCode, startTime);
        }

        // Check ticket status
        String result;
        String message;

        switch (ticket.getStatus()) {
            case "VALID":
                // Mark as USED
                ticket.setStatus("USED");
                ticketRepository.save(ticket);
                result = "SUCCESS";
                message = "✅ Vé hợp lệ - Chào mừng!";
                break;
            case "USED":
                result = "ALREADY_USED";
                message = "❌ Vé đã được sử dụng trước đó!";
                break;
            case "CANCELLED":
                result = "CANCELLED";
                message = "⚠️ Vé đã bị hủy";
                break;
            case "REFUNDED":
                result = "CANCELLED";
                message = "⚠️ Vé đã được hoàn tiền";
                break;
            default:
                result = "INVALID";
                message = "Trạng thái vé không xác định";
        }

        logScan(request, ticket, result);

        long processingTime = System.currentTimeMillis() - startTime;
        
        // Get additional info for display
        String eventName = "";
        String ticketTypeName = "";
        String seatLabel = "Standing";

        Event event = eventRepository.findById(ticket.getEventId()).orElse(null);
        if (event != null) eventName = event.getEventName();

        TicketType tt = ticketTypeRepository.findById(ticket.getTicketTypeId()).orElse(null);
        if (tt != null) ticketTypeName = tt.getTypeName();

        if (ticket.getSeatId() != null) {
            Seat seat = seatRepository.findById(ticket.getSeatId()).orElse(null);
            if (seat != null) seatLabel = seat.getSeatLabel();
        }

        log.info("Scan result: {} for ticket {} in {}ms", result, ticketCode, processingTime);

        return ScanResponse.builder()
                .result(result)
                .message(message)
                .ticketCode(ticketCode)
                .holderName(ticket.getHolderName())
                .ticketType(ticketTypeName)
                .seatLabel(seatLabel)
                .eventName(eventName)
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Đồng bộ các scan offline lên server
     */
    @Transactional
    public List<ScanResponse> syncOfflineScans(OfflineSyncRequest request) {
        List<ScanResponse> results = new ArrayList<>();
        for (ScanRequest scan : request.getScans()) {
            scan.setOfflineScan(true);
            results.add(scanTicket(scan));
        }
        return results;
    }

    /**
     * Lấy danh sách vé hợp lệ cho offline scanning
     */
    public List<String> getValidTicketCodes(Long eventId) {
        return ticketRepository.findByEventIdAndStatus(eventId, "VALID")
                .stream()
                .map(Ticket::getTicketCode)
                .toList();
    }

    private void logScan(ScanRequest request, Ticket ticket, String result) {
        LocalDateTime scannedAt = request.getScannedAt() != null ?
                LocalDateTime.parse(request.getScannedAt()) : LocalDateTime.now();

        TicketScan scan = TicketScan.builder()
                .ticketId(ticket != null ? ticket.getTicketId() : 0L)
                .scannedBy(request.getScannedBy() != null ? request.getScannedBy() : 0L)
                .eventId(request.getEventId() != null ? request.getEventId() : 
                        (ticket != null ? ticket.getEventId() : 0L))
                .scanResult(result)
                .scanLocation(request.getScanLocation())
                .deviceId(request.getDeviceId())
                .scannedAt(scannedAt)
                .isOfflineScan(request.isOfflineScan() ? 1 : 0)
                .build();

        ticketScanRepository.save(scan);
    }

    private ScanResponse buildFailResponse(String result, String message, String ticketCode, long startTime) {
        return ScanResponse.builder()
                .result(result)
                .message(message)
                .ticketCode(ticketCode)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
}

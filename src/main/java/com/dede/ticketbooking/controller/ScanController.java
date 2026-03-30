package com.dede.ticketbooking.controller;

import com.dede.ticketbooking.dto.ApiResponse;
import com.dede.ticketbooking.dto.ScanDTO.*;
import com.dede.ticketbooking.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    /**
     * Quét vé QR — Target: < 5 giây/lần
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScanResponse>> scanTicket(@RequestBody ScanRequest request) {
        ScanResponse response = scanService.scanTicket(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Đồng bộ các lần quét offline
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<List<ScanResponse>>> syncOfflineScans(@RequestBody OfflineSyncRequest request) {
        List<ScanResponse> responses = scanService.syncOfflineScans(request);
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ " + responses.size() + " lần quét", responses));
    }

    /**
     * Tải danh sách vé hợp lệ cho offline scanning
     */
    @GetMapping("/event/{eventId}/valid-tickets")
    public ResponseEntity<ApiResponse<List<String>>> getValidTickets(@PathVariable Long eventId) {
        List<String> ticketCodes = scanService.getValidTicketCodes(eventId);
        return ResponseEntity.ok(ApiResponse.ok("Loaded " + ticketCodes.size() + " valid tickets", ticketCodes));
    }
}

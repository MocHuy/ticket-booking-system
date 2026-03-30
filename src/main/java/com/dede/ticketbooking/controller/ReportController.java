package com.dede.ticketbooking.controller;

import com.dede.ticketbooking.dto.ApiResponse;
import com.dede.ticketbooking.dto.ReportDTO.*;
import com.dede.ticketbooking.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<EventReportResponse>> getEventReport(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getEventReport(eventId)));
    }

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getSystemStats() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSystemStats()));
    }
}

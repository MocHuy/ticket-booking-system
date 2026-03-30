package com.dede.ticketbooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * QR Code Service — trái tim chống vé giả
 * 
 * Mỗi vé có:
 *   - ticket_code: UUID v4 (unique, không đoán được)
 *   - qr_code_data: HMAC-SHA256 signed payload
 * 
 * Payload format: ticket_code|event_id|issued_at_epoch
 * Signature: HMAC-SHA256(payload, SECRET_KEY)
 * QR data = payload + "|" + signature (Base64)
 * 
 * App soát vé verify offline:
 *   1. Parse QR data
 *   2. Tách payload và signature
 *   3. Tính lại HMAC-SHA256(payload, SECRET_KEY)
 *   4. So sánh signature → nếu khớp = vé thật
 */
@Service
public class QRCodeService {

    @Value("${app.qrcode.secret}")
    private String qrSecret;

    public String generateTicketCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public String generateQRCodeData(String ticketCode, Long eventId, long issuedAtEpoch) {
        String payload = ticketCode + "|" + eventId + "|" + issuedAtEpoch;
        String signature = hmacSha256(payload);
        return payload + "|" + signature;
    }

    public boolean verifyQRCodeData(String qrCodeData) {
        try {
            String[] parts = qrCodeData.split("\\|");
            if (parts.length != 4) return false;

            String payload = parts[0] + "|" + parts[1] + "|" + parts[2];
            String providedSignature = parts[3];
            String expectedSignature = hmacSha256(payload);

            return providedSignature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractTicketCode(String qrCodeData) {
        String[] parts = qrCodeData.split("\\|");
        return parts.length >= 1 ? parts[0] : null;
    }

    public Long extractEventId(String qrCodeData) {
        try {
            String[] parts = qrCodeData.split("\\|");
            return parts.length >= 2 ? Long.parseLong(parts[1]) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256", e);
        }
    }
}

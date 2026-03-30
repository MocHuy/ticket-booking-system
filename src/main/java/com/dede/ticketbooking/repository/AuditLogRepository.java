package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}

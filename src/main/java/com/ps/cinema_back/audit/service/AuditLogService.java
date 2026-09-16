package com.ps.cinema_back.audit.service;

public interface AuditLogService {
    void logAction(String action, String description);
}

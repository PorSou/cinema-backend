package com.ps.cinema_back.audit.service.impl;

import com.ps.cinema_back.audit.entity.AuditLog;
import com.ps.cinema_back.audit.repository.AuditLogRepository;
import com.ps.cinema_back.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String action, String description) {
        String userEmail = "ANONYMOUS";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                userEmail = auth.getName();
            }
        } catch (Exception ignored) {}

        String ipAddress = "127.0.0.1";
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest request = attr.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
            }
        } catch (Exception ignored) {}

        AuditLog log = AuditLog.builder()
                .userEmail(userEmail)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }
}

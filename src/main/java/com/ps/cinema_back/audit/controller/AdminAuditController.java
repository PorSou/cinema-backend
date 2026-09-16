package com.ps.cinema_back.audit.controller;

import com.ps.cinema_back.audit.entity.AuditLog;
import com.ps.cinema_back.audit.repository.AuditLogRepository;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "Endpoints for monitoring system audit logs and activities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController extends BaseController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Get paginated system audit logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return OK(PageResponse.of(auditLogRepository.findAll(pageable)), "Audit logs fetched successfully");
    }
}
package com.ps.cinema_back.settings.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.settings.entity.AppSetting;
import com.ps.cinema_back.settings.repository.AppSettingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "App Settings Management", description = "Endpoints for managing global public application settings like branding logo")
public class AppSettingController extends BaseController {

    private final AppSettingRepository appSettingRepository;

    @GetMapping("/public")
    @Operation(summary = "Get all public application settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicSettings() {
        Map<String, String> settings = new HashMap<>();
        appSettingRepository.findAll().forEach(s -> {
            if (s.getKey() != null && s.getValue() != null) {
                settings.put(s.getKey(), s.getValue());
            }
        });
        return OK(settings, "Public settings fetched successfully");
    }

    @PutMapping("/update")
    @Operation(summary = "Update global application settings (e.g., logo URL)")
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, String> newSettings) {
        newSettings.forEach((key, value) -> {
            AppSetting setting = appSettingRepository.findByKey(key).orElse(new AppSetting());
            setting.setKey(key);
            setting.setValue(value);
            appSettingRepository.save(setting);
        });
        return OK(null, "Settings updated successfully");
    }
}
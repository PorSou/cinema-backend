package com.ps.cinema_back.cloudinary.controller;

import com.ps.cinema_back.cloudinary.service.CloudinaryService;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cloudinary")
@RequiredArgsConstructor
@Tag(name = "Cloudinary Management", description = "Endpoints for uploading images directly to Cloudinary")
public class CloudinaryController extends BaseController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image file to Cloudinary and return the secure URL")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String imageUrl = cloudinaryService.uploadImage(file);

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new RuntimeException("Failed to obtain secure URL from Cloudinary");
        }

        return OK(imageUrl, "Image uploaded to Cloudinary successfully");
    }
}
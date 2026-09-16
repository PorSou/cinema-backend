package com.ps.cinema_back.concession.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.concession.dto.request.ConcessionItemRequest;
import com.ps.cinema_back.concession.dto.response.ConcessionItemResponse;
import com.ps.cinema_back.concession.service.ConcessionItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concessions")
@RequiredArgsConstructor
@Tag(name = "Concession F&B Management", description = "Endpoints for managing customer combos, snacks, drinks, image uploads, and trash bin")
public class ConcessionItemController extends BaseController {

    private final ConcessionItemService concessionItemService;

    @GetMapping
    @Operation(summary = "Get all active concession items for customer checkout flow")
    public ResponseEntity<ApiResponse<List<ConcessionItemResponse>>> getActiveCustomerItems() {
        return OK(concessionItemService.getActiveCustomerItems(), "Active concessions fetched successfully");
    }

    @GetMapping("/admin")
    @Operation(summary = "Get paginated active concession items for admin panel")
    public ResponseEntity<ApiResponse<PageResponse<ConcessionItemResponse>>> getAllAdminItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(concessionItemService.getAllAdminItems(pageable)), "Admin concessions fetched successfully");
    }

    @GetMapping("/trash")
    @Operation(summary = "Get paginated deleted concession items in trash")
    public ResponseEntity<ApiResponse<PageResponse<ConcessionItemResponse>>> getTrashItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(concessionItemService.getTrashItems(pageable)), "Trash concessions fetched successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get concession item by ID")
    public ResponseEntity<ApiResponse<ConcessionItemResponse>> getItemById(@PathVariable Long id) {
        return OK(concessionItemService.getItemById(id), "Concession item retrieved successfully");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create concession item with Cloudinary image upload")
    public ResponseEntity<ApiResponse<ConcessionItemResponse>> createItem(
            @Valid @RequestPart("data") ConcessionItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return CREATED(concessionItemService.createItem(request, imageFile), "Concession item created successfully");
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update concession item with optional new image upload")
    public ResponseEntity<ApiResponse<ConcessionItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestPart("data") ConcessionItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return OK(concessionItemService.updateItem(id, request, imageFile), "Concession item updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete concession item (Move to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteItem(@PathVariable Long id) {
        concessionItemService.softDeleteItem(id);
        return OK(null, "Concession item moved to trash successfully");
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore concession item from trash")
    public ResponseEntity<ApiResponse<ConcessionItemResponse>> restoreItem(@PathVariable Long id) {
        return OK(concessionItemService.restoreItem(id), "Concession item restored successfully");
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Permanently delete concession item")
    public ResponseEntity<ApiResponse<Void>> hardDeleteItem(@PathVariable Long id) {
        concessionItemService.hardDeleteItem(id);
        return OK(null, "Concession item permanently deleted");
    }
}
package com.ps.cinema_back.concession.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.concession.dto.request.CategoryRequest;
import com.ps.cinema_back.concession.dto.response.CategoryResponse;
import com.ps.cinema_back.concession.service.ConcessionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concession-categories")
@RequiredArgsConstructor
@Tag(name = "Concession Category Management", description = "Endpoints for managing F&B categories, trash bin, and hard deletes")
public class ConcessionCategoryController extends BaseController {

    private final ConcessionCategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get paginated active concession categories")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(categoryService.getAllCategories(pageable)), "Categories fetched successfully");
    }

    @GetMapping("/list")
    @Operation(summary = "Get full active categories list for dropdowns")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategoriesList() {
        return OK(categoryService.getAllActiveCategoriesList(), "Active categories list fetched successfully");
    }

    @GetMapping("/trash")
    @Operation(summary = "Get paginated deleted categories in trash")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getTrashCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(categoryService.getTrashCategories(pageable)), "Trash categories fetched successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return OK(categoryService.getCategoryById(id), "Category retrieved successfully");
    }

    @PostMapping
    @Operation(summary = "Create a new concession category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return CREATED(categoryService.createCategory(request), "Category created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return OK(categoryService.updateCategory(id, request), "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete category (Move to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteCategory(@PathVariable Long id) {
        categoryService.softDeleteCategory(id);
        return OK(null, "Category moved to trash successfully");
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore category from trash")
    public ResponseEntity<ApiResponse<CategoryResponse>> restoreCategory(@PathVariable Long id) {
        return OK(categoryService.restoreCategory(id), "Category restored successfully");
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Permanently delete category")
    public ResponseEntity<ApiResponse<Void>> hardDeleteCategory(@PathVariable Long id) {
        categoryService.hardDeleteCategory(id);
        return OK(null, "Category permanently deleted");
    }
}
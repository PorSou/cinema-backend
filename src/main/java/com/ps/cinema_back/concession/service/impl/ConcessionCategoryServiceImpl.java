package com.ps.cinema_back.concession.service.impl;

import com.ps.cinema_back.audit.service.AuditLogService; // 👈 Added AuditLogService import
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.concession.dto.request.CategoryRequest;
import com.ps.cinema_back.concession.dto.response.CategoryResponse;
import com.ps.cinema_back.concession.entity.ConcessionCategory;
import com.ps.cinema_back.concession.repository.ConcessionCategoryRepository;
import com.ps.cinema_back.concession.service.ConcessionCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcessionCategoryServiceImpl implements ConcessionCategoryService {

    private final ConcessionCategoryRepository categoryRepository;
    private final AuditLogService auditLogService; // 👈 Injected AuditLogService

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategoriesList() {
        return categoryRepository.findAllByIsDeletedFalseOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getTrashCategories(Pageable pageable) {
        return categoryRepository.findAllByIsDeletedTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        ConcessionCategory category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(trimmedName)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists.");
        }

        ConcessionCategory category = ConcessionCategory.builder()
                .name(trimmedName)
                .description(request.getDescription())
                .isDeleted(false)
                .build();

        ConcessionCategory savedCategory = categoryRepository.save(category);

        // 👈 Catch and log CREATE category action
        auditLogService.logAction(
                "CREATE_CONCESSION_CATEGORY",
                "Created F&B category: '" + savedCategory.getName() + "' (ID: " + savedCategory.getId() + ")"
        );

        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        ConcessionCategory category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession category not found with id: " + id));

        String oldName = category.getName();
        String newName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName) &&
                categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(newName)) {
            throw new ConflictException("Category with name '" + newName + "' already exists.");
        }

        category.setName(newName);
        category.setDescription(request.getDescription());

        ConcessionCategory updatedCategory = categoryRepository.save(category);

        // 👈 Catch and log UPDATE category action
        auditLogService.logAction(
                "UPDATE_CONCESSION_CATEGORY",
                "Updated F&B category ID " + id + " from name '" + oldName + "' to '" + updatedCategory.getName() + "'"
        );

        return mapToResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void softDeleteCategory(Long id) {
        ConcessionCategory category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession category not found with id: " + id));
        category.setIsDeleted(true);
        categoryRepository.save(category);

        // 👈 Catch and log SOFT DELETE category action
        auditLogService.logAction(
                "SOFT_DELETE_CONCESSION_CATEGORY",
                "Moved F&B category '" + category.getName() + "' (ID: " + id + ") to trash"
        );
    }

    @Override
    @Transactional
    public CategoryResponse restoreCategory(Long id) {
        ConcessionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession category not found with id: " + id));

        if (!Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BadRequestException("Category with id " + id + " is not in trash");
        }

        if (categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(category.getName())) {
            throw new ConflictException("Cannot restore. Active category with name '" + category.getName() + "' already exists");
        }

        category.setIsDeleted(false);
        ConcessionCategory restoredCategory = categoryRepository.save(category);

        // 👈 Catch and log RESTORE category action
        auditLogService.logAction(
                "RESTORE_CONCESSION_CATEGORY",
                "Restored F&B category '" + restoredCategory.getName() + "' (ID: " + id + ") from trash"
        );

        return mapToResponse(restoredCategory);
    }

    @Override
    @Transactional
    public void hardDeleteCategory(Long id) {
        ConcessionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession category not found with id: " + id));

        String categoryName = category.getName();
        categoryRepository.delete(category);

        // 👈 Catch and log HARD DELETE category action
        auditLogService.logAction(
                "HARD_DELETE_CONCESSION_CATEGORY",
                "Permanently deleted F&B category '" + categoryName + "' (ID: " + id + ")"
        );
    }

    private CategoryResponse mapToResponse(ConcessionCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .itemCount(category.getItems() != null ? (int) category.getItems().stream().filter(i -> !Boolean.TRUE.equals(i.getIsDeleted())).count() : 0)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
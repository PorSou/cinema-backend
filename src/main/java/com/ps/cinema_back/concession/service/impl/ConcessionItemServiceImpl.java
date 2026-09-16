package com.ps.cinema_back.concession.service.impl;

import com.ps.cinema_back.audit.service.AuditLogService; // 👈 Added AuditLogService import
import com.ps.cinema_back.cloudinary.service.CloudinaryService;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.concession.dto.request.ConcessionItemRequest;
import com.ps.cinema_back.concession.dto.response.ConcessionItemResponse;
import com.ps.cinema_back.concession.entity.ConcessionCategory;
import com.ps.cinema_back.concession.entity.ConcessionItem;
import com.ps.cinema_back.concession.repository.ConcessionCategoryRepository;
import com.ps.cinema_back.concession.repository.ConcessionItemRepository;
import com.ps.cinema_back.concession.service.ConcessionItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcessionItemServiceImpl implements ConcessionItemService {

    private final ConcessionItemRepository itemRepository;
    private final ConcessionCategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService; // 👈 Injected AuditLogService

    @Override
    @Transactional(readOnly = true)
    public List<ConcessionItemResponse> getActiveCustomerItems() {
        return itemRepository.findByIsDeletedFalseAndActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConcessionItemResponse> getAllAdminItems(Pageable pageable) {
        return itemRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConcessionItemResponse> getTrashItems(Pageable pageable) {
        return itemRepository.findAllByIsDeletedTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ConcessionItemResponse getItemById(Long id) {
        ConcessionItem item = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession item not found with id: " + id));
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public ConcessionItemResponse createItem(ConcessionItemRequest request, MultipartFile imageFile) {
        ConcessionCategory category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(imageFile);
        }

        ConcessionItem item = ConcessionItem.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .active(request.isActive())
                .imageUrl(imageUrl)
                .category(category)
                .isDeleted(false)
                .build();

        ConcessionItem savedItem = itemRepository.save(item);

        // 👈 Catch and log CREATE concession item action
        auditLogService.logAction(
                "CREATE_CONCESSION_ITEM",
                "Created F&B item: '" + savedItem.getName() + "' under category '" + category.getName() + "' (Price: $" + savedItem.getPrice() + ")"
        );

        return mapToResponse(savedItem);
    }

    @Override
    @Transactional
    public ConcessionItemResponse updateItem(Long id, ConcessionItemRequest request, MultipartFile imageFile) {
        ConcessionItem item = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession item not found with id: " + id));

        ConcessionCategory category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String oldName = item.getName();

        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setActive(request.isActive());
        item.setCategory(category);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(imageFile);
            item.setImageUrl(imageUrl);
        }

        ConcessionItem updatedItem = itemRepository.save(item);

        // 👈 Catch and log UPDATE concession item action
        auditLogService.logAction(
                "UPDATE_CONCESSION_ITEM",
                "Updated F&B item ID " + id + " from name '" + oldName + "' to '" + updatedItem.getName() + "'"
        );

        return mapToResponse(updatedItem);
    }

    @Override
    @Transactional
    public void softDeleteItem(Long id) {
        ConcessionItem item = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession item not found with id: " + id));
        item.setIsDeleted(true);
        itemRepository.save(item);

        // 👈 Catch and log SOFT DELETE concession item action
        auditLogService.logAction(
                "SOFT_DELETE_CONCESSION_ITEM",
                "Moved F&B item '" + item.getName() + "' (ID: " + id + ") to trash"
        );
    }

    @Override
    @Transactional
    public ConcessionItemResponse restoreItem(Long id) {
        ConcessionItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession item not found with id: " + id));

        if (!Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new BadRequestException("Concession item with id " + id + " is not in trash");
        }

        item.setIsDeleted(false);
        ConcessionItem restoredItem = itemRepository.save(item);

        // 👈 Catch and log RESTORE concession item action
        auditLogService.logAction(
                "RESTORE_CONCESSION_ITEM",
                "Restored F&B item '" + restoredItem.getName() + "' (ID: " + id + ") from trash"
        );

        return mapToResponse(restoredItem);
    }

    @Override
    @Transactional
    public void hardDeleteItem(Long id) {
        ConcessionItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concession item not found with id: " + id));

        String itemName = item.getName();
        itemRepository.delete(item);

        // 👈 Catch and log HARD DELETE concession item action
        auditLogService.logAction(
                "HARD_DELETE_CONCESSION_ITEM",
                "Permanently deleted F&B item '" + itemName + "' (ID: " + id + ")"
        );
    }

    private ConcessionItemResponse mapToResponse(ConcessionItem item) {
        String fullImageUrl = null;
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            if (item.getImageUrl().startsWith("http://") || item.getImageUrl().startsWith("https://")) {
                fullImageUrl = item.getImageUrl();
            } else {
                try {
                    fullImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/")
                            .path(item.getImageUrl().startsWith("/") ? item.getImageUrl().substring(1) : item.getImageUrl())
                            .toUriString();
                } catch (Exception e) {
                    fullImageUrl = item.getImageUrl();
                }
            }
        }

        return ConcessionItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(fullImageUrl)
                .active(item.isActive())
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
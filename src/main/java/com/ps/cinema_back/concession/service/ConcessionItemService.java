package com.ps.cinema_back.concession.service;

import com.ps.cinema_back.concession.dto.request.ConcessionItemRequest;
import com.ps.cinema_back.concession.dto.response.ConcessionItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConcessionItemService {
    List<ConcessionItemResponse> getActiveCustomerItems();
    Page<ConcessionItemResponse> getAllAdminItems(Pageable pageable);
    Page<ConcessionItemResponse> getTrashItems(Pageable pageable);
    ConcessionItemResponse getItemById(Long id);
    ConcessionItemResponse createItem(ConcessionItemRequest request, MultipartFile imageFile);
    ConcessionItemResponse updateItem(Long id, ConcessionItemRequest request, MultipartFile imageFile);
    void softDeleteItem(Long id);
    ConcessionItemResponse restoreItem(Long id);
    void hardDeleteItem(Long id);
}
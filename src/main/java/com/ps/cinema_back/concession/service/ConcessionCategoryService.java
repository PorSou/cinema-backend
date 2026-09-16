package com.ps.cinema_back.concession.service;

import com.ps.cinema_back.concession.dto.request.CategoryRequest;
import com.ps.cinema_back.concession.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConcessionCategoryService {
    Page<CategoryResponse> getAllCategories(Pageable pageable);
    List<CategoryResponse> getAllActiveCategoriesList();
    Page<CategoryResponse> getTrashCategories(Pageable pageable);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void softDeleteCategory(Long id);
    CategoryResponse restoreCategory(Long id);
    void hardDeleteCategory(Long id);
}
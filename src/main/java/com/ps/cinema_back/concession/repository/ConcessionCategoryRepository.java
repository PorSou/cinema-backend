package com.ps.cinema_back.concession.repository;

import com.ps.cinema_back.concession.entity.ConcessionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcessionCategoryRepository extends JpaRepository<ConcessionCategory, Long> {
    Optional<ConcessionCategory> findByIdAndIsDeletedFalse(Long id);
    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
    Page<ConcessionCategory> findAllByIsDeletedFalse(Pageable pageable);
    List<ConcessionCategory> findAllByIsDeletedFalseOrderByNameAsc();
    Page<ConcessionCategory> findAllByIsDeletedTrue(Pageable pageable);
}
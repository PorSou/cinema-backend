package com.ps.cinema_back.concession.repository;

import com.ps.cinema_back.concession.entity.ConcessionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcessionItemRepository extends JpaRepository<ConcessionItem, Long> {
    Optional<ConcessionItem> findByIdAndIsDeletedFalse(Long id);
    Page<ConcessionItem> findAllByIsDeletedFalse(Pageable pageable);
    List<ConcessionItem> findByIsDeletedFalseAndActiveTrue();
    List<ConcessionItem> findByCategoryIdAndIsDeletedFalseAndActiveTrue(Long categoryId);
    Page<ConcessionItem> findAllByIsDeletedTrue(Pageable pageable);
}
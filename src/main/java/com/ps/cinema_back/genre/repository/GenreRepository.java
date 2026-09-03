package com.ps.cinema_back.genre.repository;

import com.ps.cinema_back.genre.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    // Active records
    Optional<Genre> findByIdAndIsDeletedFalse(Long id);
    Optional<Genre> findByNameIgnoreCaseAndIsDeletedFalse(String name);
    Page<Genre> findAllByIsDeletedFalse(Pageable pageable);
    List<Genre> findAllByIsDeletedFalseOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    // Trash / Deleted records
    Page<Genre> findAllByIsDeletedTrue(Pageable pageable);
}
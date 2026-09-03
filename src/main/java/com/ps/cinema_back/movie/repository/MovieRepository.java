package com.ps.cinema_back.movie.repository;

import com.ps.cinema_back.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    Optional<Movie> findByIdAndIsDeletedFalse(Long id);

    boolean existsByTitleIgnoreCaseAndIsDeletedFalse(String title);

    Page<Movie> findAllByIsDeletedTrue(Pageable pageable);
}
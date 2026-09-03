package com.ps.cinema_back.user.repository;

import com.ps.cinema_back.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    List<User> findAllByIsDeletedFalse();

    List<User> findAllByIsDeletedTrue();

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    Page<User> findAllByIsDeletedTrue(Pageable pageable);
}
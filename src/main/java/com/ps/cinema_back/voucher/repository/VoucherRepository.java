package com.ps.cinema_back.voucher.repository;

import com.ps.cinema_back.voucher.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeAndIsDeletedFalse(String code);
    Page<Voucher> findAllByIsDeletedFalse(Pageable pageable);
    boolean existsByCodeAndIsDeletedFalse(String code);
}
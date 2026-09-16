package com.ps.cinema_back.concession.repository;

import com.ps.cinema_back.concession.entity.BookingConcessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingConcessionItemRepository extends JpaRepository<BookingConcessionItem, Long> {
    List<BookingConcessionItem> findByBookingId(Long bookingId);
}
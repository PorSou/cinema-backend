package com.ps.cinema_back.showtime.repository;

import com.ps.cinema_back.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {

    Optional<Showtime> findByIdAndIsDeletedFalse(Long id);

    Page<Showtime> findAllByIsDeletedFalse(Pageable pageable);

    List<Showtime> findAllByMovieIdAndIsDeletedFalseOrderByStartTimeAsc(Long movieId);

    List<Showtime> findAllByHallIdAndIsDeletedFalseOrderByStartTimeAsc(Long hallId);

    // Scoped to hall AND movie: two DIFFERENT movies may overlap in the same hall,
    // but the SAME movie must respect the buffer against its own other showings there.
    @Query("""
        SELECT COUNT(s) > 0 FROM Showtime s
        WHERE s.hall.id = :hallId
          AND s.movie.id = :movieId
          AND s.isDeleted = false
          AND (:startTime < s.endTime AND :endTime > s.startTime)
    """)
    boolean existsOverlappingShowtime(
            @Param("hallId") Long hallId,
            @Param("movieId") Long movieId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT COUNT(s) > 0 FROM Showtime s
        WHERE s.hall.id = :hallId
          AND s.movie.id = :movieId
          AND s.id <> :showtimeId
          AND s.isDeleted = false
          AND (:startTime < s.endTime AND :endTime > s.startTime)
    """)
    boolean existsOverlappingShowtimeExcludingSelf(
            @Param("hallId") Long hallId,
            @Param("movieId") Long movieId,
            @Param("showtimeId") Long showtimeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<Showtime> findAllByMovieIdAndStartTimeBetweenAndIsDeletedFalseOrderByStartTimeAsc(
            Long movieId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
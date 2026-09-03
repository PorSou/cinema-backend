package com.ps.cinema_back.showtime.specification;

import com.ps.cinema_back.showtime.entity.Showtime;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeSpecification {

    public static Specification<Showtime> filterShowtimes(
            Long movieId,
            Long cinemaId,
            Long hallId,
            LocalDate date,
            boolean isDeleted) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));

            if (movieId != null) {
                predicates.add(criteriaBuilder.equal(root.get("movie").get("id"), movieId));
            }

            if (hallId != null) {
                predicates.add(criteriaBuilder.equal(root.get("hall").get("id"), hallId));
            }

            if (cinemaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("hall").get("cinema").get("id"), cinemaId));
            }

            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                predicates.add(criteriaBuilder.between(root.get("startTime"), startOfDay, endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
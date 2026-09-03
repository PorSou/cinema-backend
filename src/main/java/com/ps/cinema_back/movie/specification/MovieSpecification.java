package com.ps.cinema_back.movie.specification;

import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import com.ps.cinema_back.genre.entity.Genre;
import com.ps.cinema_back.movie.entity.Movie;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MovieSpecification {

    public static Specification<Movie> filterMovies(
            MovieStatus status,
            Language language,
            Long genreId,
            String search,
            boolean isDeleted) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Soft delete
            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Language filter
            if (language != null) {
                predicates.add(criteriaBuilder.equal(root.get("language"), language));
            }

            // Genre filter
            if (genreId != null) {
                Join<Movie, Genre> genresJoin = root.join("genres");
                predicates.add(criteriaBuilder.equal(genresJoin.get("id"), genreId));
                query.distinct(true);
            }

            // Title search
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
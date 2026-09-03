package com.ps.cinema_back.movie.service;

import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import com.ps.cinema_back.movie.dto.request.MovieRequest;
import com.ps.cinema_back.movie.dto.response.MovieResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieService {

    MovieResponse createMovie(MovieRequest request);

    MovieResponse getMovieById(Long id);

    Page<MovieResponse> getAllMovies(MovieStatus status, Language language, Long genreId, String search, Pageable pageable);

    Page<MovieResponse> getTrashMovies(String search, Pageable pageable);

    MovieResponse updateMovie(Long id, MovieRequest request);

    void softDeleteMovie(Long id);

    void hardDeleteMovie(Long id);

    MovieResponse restoreMovie(Long id);
}
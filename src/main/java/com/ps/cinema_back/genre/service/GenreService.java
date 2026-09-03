package com.ps.cinema_back.genre.service;

import com.ps.cinema_back.genre.dto.request.GenreRequest;
import com.ps.cinema_back.genre.dto.response.GenreResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GenreService {

    GenreResponse createGenre(GenreRequest request);

    GenreResponse getGenreById(Long id);

    Page<GenreResponse> getAllGenres(Pageable pageable);

    List<GenreResponse> getAllActiveGenresList();

    Page<GenreResponse> getTrashGenres(Pageable pageable);

    GenreResponse updateGenre(Long id, GenreRequest request);

    void softDeleteGenre(Long id);

    void hardDeleteGenre(Long id);

    GenreResponse restoreGenre(Long id);
}
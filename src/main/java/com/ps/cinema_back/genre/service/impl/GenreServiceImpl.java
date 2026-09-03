package com.ps.cinema_back.genre.service.impl;

import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.genre.dto.request.GenreRequest;
import com.ps.cinema_back.genre.dto.response.GenreResponse;
import com.ps.cinema_back.genre.entity.Genre;
import com.ps.cinema_back.genre.repository.GenreRepository;
import com.ps.cinema_back.genre.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        String trimmedName = request.getName().trim();

        if (genreRepository.existsByNameIgnoreCaseAndIsDeletedFalse(trimmedName)) {
            throw new ConflictException("Genre with name '" + trimmedName + "' already exists");
        }

        Genre genre = Genre.builder()
                .name(trimmedName)
                .isDeleted(false)
                .build();

        return mapToGenreResponse(genreRepository.save(genre));
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Long id) {
        Genre genre = genreRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
        return mapToGenreResponse(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GenreResponse> getAllGenres(Pageable pageable) {
        return genreRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToGenreResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> getAllActiveGenresList() {
        return genreRepository.findAllByIsDeletedFalseOrderByNameAsc()
                .stream()
                .map(this::mapToGenreResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GenreResponse> getTrashGenres(Pageable pageable) {
        return genreRepository.findAllByIsDeletedTrue(pageable)
                .map(this::mapToGenreResponse);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genreRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        String newName = request.getName().trim();

        // Check if new name conflicts with an existing genre (other than itself)
        if (!genre.getName().equalsIgnoreCase(newName) &&
                genreRepository.existsByNameIgnoreCaseAndIsDeletedFalse(newName)) {
            throw new ConflictException("Genre with name '" + newName + "' already exists");
        }

        genre.setName(newName);
        return mapToGenreResponse(genreRepository.save(genre));
    }

    @Override
    @Transactional
    public void softDeleteGenre(Long id) {
        Genre genre = genreRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        genre.setIsDeleted(true);
        genreRepository.save(genre);
    }

    @Override
    @Transactional
    public void hardDeleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        genreRepository.delete(genre);
    }

    @Override
    @Transactional
    public GenreResponse restoreGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        if (!Boolean.TRUE.equals(genre.getIsDeleted())) {
            throw new BadRequestException("Genre with id " + id + " is not deleted");
        }

        if (genreRepository.existsByNameIgnoreCaseAndIsDeletedFalse(genre.getName())) {
            throw new ConflictException("Cannot restore. Active genre with name '" + genre.getName() + "' already exists");
        }

        genre.setIsDeleted(false);
        return mapToGenreResponse(genreRepository.save(genre));
    }

    private GenreResponse mapToGenreResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .createdAt(genre.getCreatedAt())
                .updatedAt(genre.getUpdatedAt())
                .build();
    }
}
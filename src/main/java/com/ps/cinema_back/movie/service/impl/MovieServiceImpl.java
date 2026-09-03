package com.ps.cinema_back.movie.service.impl;

import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.common.service.FileStorageService;
import com.ps.cinema_back.genre.dto.response.GenreResponse;
import com.ps.cinema_back.genre.entity.Genre;
import com.ps.cinema_back.genre.repository.GenreRepository;
import com.ps.cinema_back.movie.dto.request.MovieRequest;
import com.ps.cinema_back.movie.dto.response.MovieResponse;
import com.ps.cinema_back.movie.entity.Movie;
import com.ps.cinema_back.movie.repository.MovieRepository;
import com.ps.cinema_back.movie.service.MovieService;
import com.ps.cinema_back.movie.specification.MovieSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        String trimmedTitle = request.getTitle().trim();

        if (movieRepository.existsByTitleIgnoreCaseAndIsDeletedFalse(trimmedTitle)) {
            throw new ConflictException("Movie with title '" + trimmedTitle + "' already exists");
        }

        String posterUrl = null;
        if (request.getPosterFile() != null && !request.getPosterFile().isEmpty()) {
            posterUrl = fileStorageService.saveFile(request.getPosterFile());
        }

        Set<Genre> genres = fetchAndValidateGenres(request.getGenreIds());

        Movie movie = Movie.builder()
                .title(trimmedTitle)
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .status(request.getStatus())
                .language(request.getLanguage() != null ? request.getLanguage() : Language.KHMER)
                .ageRating(request.getAgeRating())
                .posterUrl(posterUrl)
                .trailerUrl(request.getTrailerUrl())
                .releaseDate(request.getReleaseDate())
                .genres(genres)
                .isDeleted(false)
                .build();

        return mapToMovieResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return mapToMovieResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllMovies(MovieStatus status, Language language, Long genreId, String search, Pageable pageable) {
        Specification<Movie> spec = MovieSpecification.filterMovies(status, language, genreId, search, false);
        return movieRepository.findAll(spec, pageable).map(this::mapToMovieResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getTrashMovies(String search, Pageable pageable) {
        // Corrected to pass all 5 arguments (status=null, language=null, genreId=null, search, isDeleted=true)
        Specification<Movie> spec = MovieSpecification.filterMovies(null, null, null, search, true);
        return movieRepository.findAll(spec, pageable).map(this::mapToMovieResponse);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        String trimmedTitle = request.getTitle().trim();

        if (!movie.getTitle().equalsIgnoreCase(trimmedTitle) &&
                movieRepository.existsByTitleIgnoreCaseAndIsDeletedFalse(trimmedTitle)) {
            throw new ConflictException("Movie with title '" + trimmedTitle + "' already exists");
        }

        if (request.getPosterFile() != null && !request.getPosterFile().isEmpty()) {
            String newPosterUrl = fileStorageService.saveFile(request.getPosterFile());
            movie.setPosterUrl(newPosterUrl);
        }

        if (request.getGenreIds() != null) {
            Set<Genre> genres = fetchAndValidateGenres(request.getGenreIds());
            movie.setGenres(genres);
        }

        movie.setTitle(trimmedTitle);
        movie.setDescription(request.getDescription());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setStatus(request.getStatus());
        movie.setLanguage(request.getLanguage() != null ? request.getLanguage() : movie.getLanguage());
        movie.setAgeRating(request.getAgeRating());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setReleaseDate(request.getReleaseDate());

        return mapToMovieResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public void softDeleteMovie(Long id) {
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        movie.setIsDeleted(true);
        movieRepository.save(movie);
    }

    @Override
    @Transactional
    public void hardDeleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        movieRepository.delete(movie);
    }

    @Override
    @Transactional
    public MovieResponse restoreMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        if (!Boolean.TRUE.equals(movie.getIsDeleted())) {
            throw new BadRequestException("Movie with id " + id + " is not deleted");
        }

        if (movieRepository.existsByTitleIgnoreCaseAndIsDeletedFalse(movie.getTitle())) {
            throw new ConflictException("Cannot restore. An active movie with title '" + movie.getTitle() + "' already exists");
        }

        movie.setIsDeleted(false);
        return mapToMovieResponse(movieRepository.save(movie));
    }

    private Set<Genre> fetchAndValidateGenres(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Genre> fetchedGenres = genreRepository.findAllById(genreIds);

        Set<Genre> activeGenres = fetchedGenres.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .collect(Collectors.toSet());

        if (activeGenres.size() != genreIds.size()) {
            throw new ResourceNotFoundException("One or more selected genres are invalid or have been deleted");
        }

        return activeGenres;
    }

    private MovieResponse mapToMovieResponse(Movie movie) {
        Set<GenreResponse> genreResponses = movie.getGenres() == null ? new HashSet<>() :
                movie.getGenres().stream()
                        .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                        .map(g -> GenreResponse.builder()
                                .id(g.getId())
                                .name(g.getName())
                                .build())
                        .collect(Collectors.toSet());

        String fullPosterUrl = null;
        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            if (movie.getPosterUrl().startsWith("http://") || movie.getPosterUrl().startsWith("https://")) {
                fullPosterUrl = movie.getPosterUrl();
            } else {
                try {
                    fullPosterUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/")
                            .path(movie.getPosterUrl().startsWith("/") ? movie.getPosterUrl().substring(1) : movie.getPosterUrl())
                            .toUriString();
                } catch (Exception e) {
                    fullPosterUrl = movie.getPosterUrl();
                }
            }
        }

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .status(movie.getStatus())
                .language(movie.getLanguage())
                .ageRating(movie.getAgeRating())
                .posterUrl(fullPosterUrl)
                .trailerUrl(movie.getTrailerUrl())
                .releaseDate(movie.getReleaseDate())
                .genres(genreResponses)
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}
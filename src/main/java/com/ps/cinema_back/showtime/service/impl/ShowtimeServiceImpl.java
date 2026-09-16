package com.ps.cinema_back.showtime.service.impl;

import com.ps.cinema_back.audit.service.AuditLogService; // 👈 Added AuditLogService import
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.hall.entity.Hall;
import com.ps.cinema_back.hall.repository.HallRepository;
import com.ps.cinema_back.movie.entity.Movie;
import com.ps.cinema_back.movie.repository.MovieRepository;
import com.ps.cinema_back.showtime.dto.request.ShowtimeRequest;
import com.ps.cinema_back.showtime.dto.response.ShowtimeResponse;
import com.ps.cinema_back.showtime.entity.Showtime;
import com.ps.cinema_back.showtime.repository.ShowtimeRepository;
import com.ps.cinema_back.showtime.service.ShowtimeService;
import com.ps.cinema_back.showtime.specification.ShowtimeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService; // 👈 Injected AuditLogService

    private static final int CLEANING_BUFFER_MINUTES = 15;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Long defaultHallId = (request.getHallIds() != null && !request.getHallIds().isEmpty())
                ? request.getHallIds().get(0)
                : null;

        if (defaultHallId == null) {
            throw new BadRequestException("Screening hall is required");
        }

        Movie movie = movieRepository.findByIdAndIsDeletedFalse(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        Hall hall = hallRepository.findByIdAndIsDeletedFalse(defaultHallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + defaultHallId));

        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes())
                .plusMinutes(CLEANING_BUFFER_MINUTES);

        if (showtimeRepository.existsOverlappingShowtime(hall.getId(), movie.getId(), request.getStartTime(), endTime)) {
            throw new ConflictException("Scheduling conflict: '" + movie.getTitle() + "' already has a showtime scheduled in Hall '" + hall.getName() + "' during this timeframe.");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .hall(hall)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .basePrice(request.getBasePrice())
                .isDeleted(false)
                .build();

        Showtime savedShowtime = showtimeRepository.save(showtime);

        // 👈 Catch and log CREATE action
        auditLogService.logAction(
                "CREATE_SHOWTIME",
                "Scheduled showtime for movie '" + movie.getTitle() + "' in Hall '" + hall.getName() + "' at " + request.getStartTime()
        );

        return mapToResponse(savedShowtime);
    }

    @Override
    @Transactional
    public List<ShowtimeResponse> createBatchShowtimes(List<ShowtimeRequest> requests) {
        List<Showtime> allSavedShowtimes = new ArrayList<>();

        for (ShowtimeRequest request : requests) {
            Movie movie = movieRepository.findByIdAndIsDeletedFalse(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

            int repeatCount = (request.getRepeatDays() != null && request.getRepeatDays() > 0)
                    ? request.getRepeatDays()
                    : 1;

            List<Long> targetHallIds;
            if (request.getHallIds() != null && !request.getHallIds().isEmpty()) {
                targetHallIds = request.getHallIds();
            } else if (request.getHallId() != null) {
                targetHallIds = java.util.Collections.singletonList(request.getHallId());
            } else {
                throw new BadRequestException("Screening hall is required");
            }

            for (Long hallId : targetHallIds) {
                Hall hall = hallRepository.findByIdAndIsDeletedFalse(hallId)
                        .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + hallId));

                for (int day = 0; day < repeatCount; day++) {
                    LocalDateTime adjustedStartTime = request.getStartTime().plusDays(day);
                    LocalDateTime endTime = adjustedStartTime
                            .plusMinutes(movie.getDurationMinutes())
                            .plusMinutes(CLEANING_BUFFER_MINUTES);

                    if (showtimeRepository.existsOverlappingShowtime(hall.getId(), movie.getId(), adjustedStartTime, endTime)) {
                        throw new ConflictException("Scheduling conflict for '" + movie.getTitle() + "' in Hall '" + hall.getName() + "' on " + adjustedStartTime.toLocalDate() + ".");
                    }

                    Showtime showtime = Showtime.builder()
                            .movie(movie)
                            .hall(hall)
                            .startTime(adjustedStartTime)
                            .endTime(endTime)
                            .basePrice(request.getBasePrice())
                            .isDeleted(false)
                            .build();

                    allSavedShowtimes.add(showtimeRepository.save(showtime));
                }
            }
        }

        // 👈 Catch and log BATCH CREATE action
        auditLogService.logAction(
                "CREATE_BATCH_SHOWTIMES",
                "Successfully generated " + allSavedShowtimes.size() + " batch showtimes schedule."
        );

        return allSavedShowtimes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long id) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
        return mapToResponse(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getAllShowtimes(Pageable pageable) {
        return showtimeRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getTrashShowtimes(Pageable pageable) {
        Specification<Showtime> spec = ShowtimeSpecification.filterShowtimes(null, null, null, null, true);
        return showtimeRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovie(Long movieId) {
        return showtimeRepository.findAllByMovieIdAndIsDeletedFalseOrderByStartTimeAsc(movieId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovieAndDate(Long movieId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return showtimeRepository.findAllByMovieIdAndStartTimeBetweenAndIsDeletedFalseOrderByStartTimeAsc(
                        movieId, startOfDay, endOfDay)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByHall(Long hallId) {
        return showtimeRepository.findAllByHallIdAndIsDeletedFalseOrderByStartTimeAsc(hallId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));

        Movie movie = movieRepository.findByIdAndIsDeletedFalse(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        Hall hall = hallRepository.findByIdAndIsDeletedFalse(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.getHallId()));

        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes())
                .plusMinutes(CLEANING_BUFFER_MINUTES);

        if (showtimeRepository.existsOverlappingShowtimeExcludingSelf(hall.getId(), movie.getId(), id, request.getStartTime(), endTime)) {
            throw new ConflictException("Scheduling conflict: '" + movie.getTitle() + "' already has a showtime scheduled in Hall '" + hall.getName() + "' during this timeframe (including the 15-minute buffer).");
        }

        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.getBasePrice());

        Showtime updatedShowtime = showtimeRepository.save(showtime);

        // 👈 Catch and log UPDATE action
        auditLogService.logAction(
                "UPDATE_SHOWTIME",
                "Updated showtime ID " + id + " for movie '" + movie.getTitle() + "' in Hall '" + hall.getName() + "'"
        );

        return mapToResponse(updatedShowtime);
    }

    @Override
    @Transactional
    public void softDeleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
        showtime.setIsDeleted(true);
        showtimeRepository.save(showtime);

        // 👈 Catch and log SOFT DELETE action
        auditLogService.logAction(
                "SOFT_DELETE_SHOWTIME",
                "Moved showtime ID " + id + " (Movie: " + showtime.getMovie().getTitle() + ") to trash"
        );
    }

    @Override
    @Transactional
    public void hardDeleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));

        boolean hasBookings = bookingRepository.existsByShowtimeIdAndIsDeletedFalse(id);
        if (hasBookings) {
            throw new BadRequestException("Cannot permanently delete this showtime because active customer bookings are attached to it. Please use soft delete (trash) instead.");
        }

        String movieTitle = showtime.getMovie().getTitle();
        showtimeRepository.delete(showtime);

        // 👈 Catch and log HARD DELETE action
        auditLogService.logAction(
                "HARD_DELETE_SHOWTIME",
                "Permanently deleted showtime ID " + id + " for movie '" + movieTitle + "'"
        );
    }

    @Override
    @Transactional
    public ShowtimeResponse restoreShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));

        if (!Boolean.TRUE.equals(showtime.getIsDeleted())) {
            throw new BadRequestException("Showtime with id " + id + " is not deleted");
        }

        showtime.setIsDeleted(false);
        Showtime restoredShowtime = showtimeRepository.save(showtime);

        // 👈 Catch and log RESTORE action
        auditLogService.logAction(
                "RESTORE_SHOWTIME",
                "Restored showtime ID " + id + " for movie '" + restoredShowtime.getMovie().getTitle() + "' from trash"
        );

        return mapToResponse(restoredShowtime);
    }

    private ShowtimeResponse mapToResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .movieDurationMinutes(showtime.getMovie().getDurationMinutes())
                .moviePosterUrl(showtime.getMovie().getPosterUrl())
                .hallId(showtime.getHall().getId())
                .hallName(showtime.getHall().getName())
                .hallType(showtime.getHall().getHallType())
                .cinemaId(showtime.getHall().getCinema().getId())
                .cinemaName(showtime.getHall().getCinema().getName())
                .cinemaCity(showtime.getHall().getCinema().getCity())
                .createdAt(showtime.getCreatedAt())
                .updatedAt(showtime.getUpdatedAt())
                .build();
    }
}
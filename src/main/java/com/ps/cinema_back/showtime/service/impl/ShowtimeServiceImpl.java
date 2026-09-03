package com.ps.cinema_back.showtime.service.impl;

import com.ps.cinema_back.booking.repository.BookingRepository; // 👈 Add booking repository import
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final BookingRepository bookingRepository; // 👈 Injected repository to check for dependent bookings

    private static final int CLEANING_BUFFER_MINUTES = 15;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        Hall hall = hallRepository.findByIdAndIsDeletedFalse(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.getHallId()));

        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes())
                .plusMinutes(CLEANING_BUFFER_MINUTES);

        if (showtimeRepository.existsOverlappingShowtime(hall.getId(), movie.getId(), request.getStartTime(), endTime)) {
            throw new ConflictException("Scheduling conflict: '" + movie.getTitle() + "' already has a showtime scheduled in Hall '" + hall.getName() + "' during this timeframe (including the 15-minute buffer).");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .hall(hall)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .basePrice(request.getBasePrice())
                .isDeleted(false)
                .build();

        return mapToResponse(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public List<ShowtimeResponse> createBatchShowtimes(List<ShowtimeRequest> requests) {
        return requests.stream()
                .map(this::createShowtime)
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

        return mapToResponse(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public void softDeleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
        showtime.setIsDeleted(true);
        showtimeRepository.save(showtime);
    }

    @Override
    @Transactional
    public void hardDeleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));

        // 👇 Prevent crash by checking if active customer bookings are attached to this showtime
        boolean hasBookings = bookingRepository.existsByShowtimeIdAndIsDeletedFalse(id);
        if (hasBookings) {
            throw new BadRequestException("Cannot permanently delete this showtime because active customer bookings are attached to it. Please use soft delete (trash) instead.");
        }

        showtimeRepository.delete(showtime);
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
        return mapToResponse(showtimeRepository.save(showtime));
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
package com.ps.cinema_back.showtime.service;

import com.ps.cinema_back.showtime.dto.request.ShowtimeRequest;
import com.ps.cinema_back.showtime.dto.response.ShowtimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {
    ShowtimeResponse createShowtime(ShowtimeRequest request);
    List<ShowtimeResponse> createBatchShowtimes(List<ShowtimeRequest> requests);
    ShowtimeResponse getShowtimeById(Long id);
    Page<ShowtimeResponse> getAllShowtimes(Pageable pageable);
    Page<ShowtimeResponse> getTrashShowtimes(Pageable pageable);
    List<ShowtimeResponse> getShowtimesByMovie(Long movieId);
    List<ShowtimeResponse> getShowtimesByMovieAndDate(Long movieId, LocalDate date);
    List<ShowtimeResponse> getShowtimesByHall(Long hallId);
    ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request);
    void softDeleteShowtime(Long id);
    void hardDeleteShowtime(Long id);
    ShowtimeResponse restoreShowtime(Long id);
}
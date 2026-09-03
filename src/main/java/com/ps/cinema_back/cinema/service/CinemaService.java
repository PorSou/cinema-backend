package com.ps.cinema_back.cinema.service;

import com.ps.cinema_back.cinema.dto.request.CinemaRequest;
import com.ps.cinema_back.cinema.dto.response.CinemaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CinemaService {
    CinemaResponse createCinema(CinemaRequest request);
    CinemaResponse getCinemaById(Long id);
    Page<CinemaResponse> getAllCinemas(Pageable pageable);
    Page<CinemaResponse> getTrashCinemas(Pageable pageable);
    CinemaResponse updateCinema(Long id, CinemaRequest request);
    void softDeleteCinema(Long id);
    CinemaResponse restoreCinema(Long id);
    void hardDeleteCinema(Long id);
}
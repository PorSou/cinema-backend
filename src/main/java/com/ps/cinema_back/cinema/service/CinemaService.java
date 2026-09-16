package com.ps.cinema_back.cinema.service;

import com.ps.cinema_back.cinema.dto.request.CinemaRequest;
import com.ps.cinema_back.cinema.dto.response.CinemaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CinemaService {
    CinemaResponse createCinema(CinemaRequest request, MultipartFile file);
    CinemaResponse getCinemaById(Long id);
    Page<CinemaResponse> getAllCinemas(Pageable pageable);
    Page<CinemaResponse> getTrashCinemas(Pageable pageable);
    CinemaResponse updateCinema(Long id, CinemaRequest request, MultipartFile file);
    void softDeleteCinema(Long id);
    CinemaResponse restoreCinema(Long id);
    void hardDeleteCinema(Long id);
}
package com.ps.cinema_back.hall.service;

import com.ps.cinema_back.hall.dto.request.HallRequest;
import com.ps.cinema_back.hall.dto.response.HallResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HallService {
    HallResponse createHall(HallRequest request);
    HallResponse getHallById(Long id);
    List<HallResponse> getHallsByCinemaId(Long cinemaId);
    List<HallResponse> getTrashHallsByCinemaId(Long cinemaId);
    Page<HallResponse> getHallsByCinemaIdPage(Long cinemaId, Pageable pageable);
    Page<HallResponse> getTrashHallsByCinemaIdPage(Long cinemaId, Pageable pageable);
    HallResponse updateHall(Long id, HallRequest request);
    void softDeleteHall(Long id);
    HallResponse restoreHall(Long id);
    void hardDeleteHall(Long id);
}
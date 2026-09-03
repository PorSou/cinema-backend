package com.ps.cinema_back.hall.service.impl;

import com.ps.cinema_back.cinema.entity.Cinema;
import com.ps.cinema_back.cinema.repository.CinemaRepository;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.hall.dto.request.HallRequest;
import com.ps.cinema_back.hall.dto.response.HallResponse;
import com.ps.cinema_back.hall.entity.Hall;
import com.ps.cinema_back.hall.repository.HallRepository;
import com.ps.cinema_back.hall.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HallServiceImpl implements HallService {

    private final HallRepository hallRepository;
    private final CinemaRepository cinemaRepository;

    @Override
    @Transactional
    public HallResponse createHall(HallRequest request) {
        Cinema cinema = cinemaRepository.findByIdAndIsDeletedFalse(request.getCinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + request.getCinemaId()));

        if (hallRepository.existsByNameIgnoreCaseAndCinemaIdAndIsDeletedFalse(request.getName(), cinema.getId())) {
            throw new ConflictException("Hall '" + request.getName() + "' already exists in this cinema");
        }

        Hall hall = Hall.builder()
                .name(request.getName())
                .hallType(request.getHallType())
                .totalSeats(0)
                .cinema(cinema)
                .isDeleted(false)
                .build();

        Hall savedHall = hallRepository.save(hall);

        // Increment cinema's totalHalls count
        cinema.setTotalHalls(cinema.getTotalHalls() + 1);
        cinemaRepository.save(cinema);

        return mapToResponse(savedHall);
    }

    @Override
    @Transactional(readOnly = true)
    public HallResponse getHallById(Long id) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));
        return mapToResponse(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getHallsByCinemaId(Long cinemaId) {
        return hallRepository.findAllByCinemaIdAndIsDeletedFalse(cinemaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getTrashHallsByCinemaId(Long cinemaId) {
        return hallRepository.findAllByCinemaIdAndIsDeletedTrue(cinemaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HallResponse> getHallsByCinemaIdPage(Long cinemaId, Pageable pageable) {
        return hallRepository.findAllByCinemaIdAndIsDeletedFalse(cinemaId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HallResponse> getTrashHallsByCinemaIdPage(Long cinemaId, Pageable pageable) {
        return hallRepository.findAllByCinemaIdAndIsDeletedTrue(cinemaId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public HallResponse updateHall(Long id, HallRequest request) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));

        hall.setName(request.getName());
        hall.setHallType(request.getHallType());

        return mapToResponse(hallRepository.save(hall));
    }

    @Override
    @Transactional
    public void softDeleteHall(Long id) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));

        hall.setIsDeleted(true);
        hallRepository.save(hall);

        // Decrement cinema total halls count
        Cinema cinema = hall.getCinema();
        if (cinema != null && cinema.getTotalHalls() > 0) {
            cinema.setTotalHalls(cinema.getTotalHalls() - 1);
            cinemaRepository.save(cinema);
        }
    }

    @Override
    @Transactional
    public HallResponse restoreHall(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));

        if (!Boolean.TRUE.equals(hall.getIsDeleted())) {
            throw new BadRequestException("Hall with id " + id + " is not in trash");
        }

        hall.setIsDeleted(false);
        Hall restoredHall = hallRepository.save(hall);

        // Increment cinema total halls count
        Cinema cinema = hall.getCinema();
        if (cinema != null) {
            cinema.setTotalHalls(cinema.getTotalHalls() + 1);
            cinemaRepository.save(cinema);
        }

        return mapToResponse(restoredHall);
    }

    @Override
    @Transactional
    public void hardDeleteHall(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));

        // If the hall was not already soft-deleted, adjust parent cinema counter before permanent removal
        if (!Boolean.TRUE.equals(hall.getIsDeleted())) {
            Cinema cinema = hall.getCinema();
            if (cinema != null && cinema.getTotalHalls() > 0) {
                cinema.setTotalHalls(cinema.getTotalHalls() - 1);
                cinemaRepository.save(cinema);
            }
        }

        hallRepository.delete(hall);
    }

    private HallResponse mapToResponse(Hall hall) {
        return HallResponse.builder()
                .id(hall.getId())
                .name(hall.getName())
                .hallType(hall.getHallType())
                .totalSeats(hall.getTotalSeats())
                .cinemaId(hall.getCinema().getId())
                .cinemaName(hall.getCinema().getName())
                .createdAt(hall.getCreatedAt())
                .updatedAt(hall.getUpdatedAt())
                .build();
    }
}
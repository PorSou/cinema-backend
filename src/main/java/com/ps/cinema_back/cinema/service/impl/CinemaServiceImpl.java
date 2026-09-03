package com.ps.cinema_back.cinema.service.impl;

import com.ps.cinema_back.cinema.dto.request.CinemaRequest;
import com.ps.cinema_back.cinema.dto.response.CinemaResponse;
import com.ps.cinema_back.cinema.entity.Cinema;
import com.ps.cinema_back.cinema.repository.CinemaRepository;
import com.ps.cinema_back.cinema.service.CinemaService;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        if (cinemaRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIsDeletedFalse(request.getName(), request.getCity())) {
            throw new ConflictException("Cinema '" + request.getName() + "' in " + request.getCity() + " already exists");
        }

        Cinema cinema = Cinema.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .phone(request.getPhone())
                .totalHalls(0)
                .isDeleted(false)
                .build();

        return mapToResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Long id) {
        Cinema cinema = cinemaRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));
        return mapToResponse(cinema);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CinemaResponse> getAllCinemas(Pageable pageable) {
        return cinemaRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CinemaResponse> getTrashCinemas(Pageable pageable) {
        return cinemaRepository.findAllByIsDeletedTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(Long id, CinemaRequest request) {
        Cinema cinema = cinemaRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));

        cinema.setName(request.getName());
        cinema.setCity(request.getCity());
        cinema.setAddress(request.getAddress());
        cinema.setPhone(request.getPhone());

        return mapToResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public void softDeleteCinema(Long id) {
        Cinema cinema = cinemaRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));
        cinema.setIsDeleted(true);
        cinemaRepository.save(cinema);
    }

    @Override
    @Transactional
    public CinemaResponse restoreCinema(Long id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));

        if (!Boolean.TRUE.equals(cinema.getIsDeleted())) {
            throw new BadRequestException("Cinema with id " + id + " is not in trash");
        }

        cinema.setIsDeleted(false);
        return mapToResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public void hardDeleteCinema(Long id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));
        cinemaRepository.delete(cinema);
    }

    private CinemaResponse mapToResponse(Cinema cinema) {
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .city(cinema.getCity())
                .address(cinema.getAddress())
                .phone(cinema.getPhone())
                .totalHalls(cinema.getTotalHalls())
                .createdAt(cinema.getCreatedAt())
                .updatedAt(cinema.getUpdatedAt())
                .build();
    }
}
package com.ps.cinema_back.cinema.service.impl;

import com.ps.cinema_back.cinema.dto.request.CinemaRequest;
import com.ps.cinema_back.cinema.dto.response.CinemaResponse;
import com.ps.cinema_back.cinema.entity.Cinema;
import com.ps.cinema_back.cinema.repository.CinemaRepository;
import com.ps.cinema_back.cinema.service.CinemaService;
import com.ps.cinema_back.cloudinary.service.CloudinaryService;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CloudinaryService cloudinaryService; // 👈 Replaced FileStorageService with CloudinaryService

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaRequest request, MultipartFile file) {
        if (cinemaRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIsDeletedFalse(request.getName(), request.getCity())) {
            throw new ConflictException("Cinema '" + request.getName() + "' in " + request.getCity() + " already exists");
        }

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(file); // 👈 Uploads to Cloudinary
        }

        Cinema cinema = Cinema.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .phone(request.getPhone())
                .image(imageUrl) // 👈 Saves Cloudinary secure URL
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
    public CinemaResponse updateCinema(Long id, CinemaRequest request, MultipartFile file) {
        Cinema cinema = cinemaRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + id));

        cinema.setName(request.getName());
        cinema.setCity(request.getCity());
        cinema.setAddress(request.getAddress());
        cinema.setPhone(request.getPhone());

        if (file != null && !file.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(file); // 👈 Uploads new image to Cloudinary
            cinema.setImage(imageUrl);
        }

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
        String fullImageUrl = null;
        if (cinema.getImage() != null && !cinema.getImage().isEmpty()) {
            // If it's already a full HTTP/HTTPS URL (Cloudinary), keep it as is
            if (cinema.getImage().startsWith("http://") || cinema.getImage().startsWith("https://")) {
                fullImageUrl = cinema.getImage();
            } else {
                try {
                    fullImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/")
                            .path(cinema.getImage().startsWith("/") ? cinema.getImage().substring(1) : cinema.getImage())
                            .toUriString();
                } catch (Exception e) {
                    fullImageUrl = cinema.getImage();
                }
            }
        }

        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .city(cinema.getCity())
                .address(cinema.getAddress())
                .phone(cinema.getPhone())
                .image(fullImageUrl) // 👈 Returns full Cloudinary URL
                .totalHalls(cinema.getTotalHalls())
                .createdAt(cinema.getCreatedAt())
                .updatedAt(cinema.getUpdatedAt())
                .build();
    }
}
package com.sirh.service;

import com.sirh.model.dto.ShiftDto;
import com.sirh.model.dto.ShiftRequest;
import com.sirh.model.entity.Shift;
import com.sirh.exception.ShiftNotFoundException;
import com.sirh.model.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> findAll() {
        return shiftRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ShiftDto getShiftByReference(UUID reference) {
        return toDto(getShiftEntityByReference(reference));
    }

    @Transactional
    public ShiftDto create(ShiftRequest request) {
        Shift shift = Shift.builder()
                .nom(request.nom())
                .typeRotation(request.typeRotation())
                .heureDebut(request.heureDebut())
                .heureFin(request.heureFin())
                .build();
        return toDto(shiftRepository.save(shift));
    }

    private Shift getShiftEntityByReference(UUID reference) {
        return shiftRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ShiftNotFoundException(reference));
    }

    private ShiftDto toDto(Shift s) {
        return new ShiftDto(s.getReference(), s.getNom(), s.getTypeRotation(), s.getHeureDebut(), s.getHeureFin(), s.getCreatedAt());
    }
}

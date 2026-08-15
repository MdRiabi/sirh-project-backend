package com.sirh.service;

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
    public List<Shift> findAll() {
        return shiftRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Shift getShiftByReference(UUID reference) {
        return shiftRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ShiftNotFoundException(reference));
    }

    @Transactional
    public Shift create(ShiftRequest request) {
        Shift shift = Shift.builder()
                .nom(request.nom())
                .typeRotation(request.typeRotation())
                .heureDebut(request.heureDebut())
                .heureFin(request.heureFin())
                .build();
        return shiftRepository.save(shift);
    }
}
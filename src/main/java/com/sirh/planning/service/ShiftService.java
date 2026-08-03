package com.sirh.planning.service;

import com.sirh.planning.dto.ShiftRequest;
import com.sirh.planning.entity.Shift;
import com.sirh.planning.exception.ShiftNotFoundException;
import com.sirh.planning.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public Shift findById(Long id) {
        return shiftRepository.findById(id).orElseThrow(() -> new ShiftNotFoundException(id));
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
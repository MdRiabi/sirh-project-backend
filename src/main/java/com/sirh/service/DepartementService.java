package com.sirh.service;

import com.sirh.exception.ResourceNotFoundException;
import com.sirh.exception.ErrorCode;

import com.sirh.model.dto.DepartementDto;
import com.sirh.model.dto.DepartementRequest;
import com.sirh.model.entity.Departement;
import com.sirh.model.repository.DepartementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartementService {

    private final DepartementRepository departementRepository;

    public DepartementService(DepartementRepository departementRepository) {
        this.departementRepository = departementRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartementDto> findAll() {
        return departementRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DepartementDto getDepartementByReference(UUID reference) {
        return toDto(getDepartementEntityByReference(reference));
    }

    @Transactional
    public DepartementDto create(DepartementRequest request) {
        Departement departement = Departement.builder()
                .nom(request.nom())
                .managerId(request.managerId())
                .build();
        return toDto(departementRepository.save(departement));
    }

    @Transactional
    public DepartementDto updateDepartementByReference(UUID reference, DepartementRequest request) {
        Departement departement = getDepartementEntityByReference(reference);
        departement.setNom(request.nom());
        departement.setManagerId(request.managerId());
        return toDto(departementRepository.save(departement));
    }

    private Departement getDepartementEntityByReference(UUID reference) {
        return departementRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPARTEMENT_NOT_FOUND, new Object[]{reference}));
    }

    private DepartementDto toDto(Departement departement) {
        return new DepartementDto(
                departement.getReference(),
                departement.getNom(),
                departement.getManagerId(),
                departement.getCreatedAt()
        );
    }
}

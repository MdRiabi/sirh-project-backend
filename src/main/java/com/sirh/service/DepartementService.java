package com.sirh.service;

import com.sirh.model.dto.DepartementRequest;
import com.sirh.model.entity.Departement;
import com.sirh.exception.DepartementNotFoundException;
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
    public List<Departement> findAll() {
        return departementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Departement getDepartementByReference(UUID reference) {
        return departementRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new DepartementNotFoundException(reference));
    }

    @Transactional
    public Departement create(DepartementRequest request) {
        Departement departement = Departement.builder()
                .nom(request.nom())
                .managerId(request.managerId())
                .build();
        return departementRepository.save(departement);
    }

    @Transactional
    public Departement updateDepartementByReference(UUID reference, DepartementRequest request) {
        Departement departement = getDepartementByReference(reference);
        departement.setNom(request.nom());
        departement.setManagerId(request.managerId());
        return departementRepository.save(departement);
    }
}

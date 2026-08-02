package com.sirh.personnel.service;

import com.sirh.personnel.dto.DepartementRequest;
import com.sirh.personnel.entity.Departement;
import com.sirh.personnel.exception.DepartementNotFoundException;
import com.sirh.personnel.repository.DepartementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public Departement findById(Long id) {
        return departementRepository.findById(id).orElseThrow(() -> new DepartementNotFoundException(id));
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
    public Departement update(Long id, DepartementRequest request) {
        Departement departement = findById(id);
        departement.setNom(request.nom());
        departement.setManagerId(request.managerId());
        return departementRepository.save(departement);
    }
}

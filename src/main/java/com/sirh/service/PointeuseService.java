package com.sirh.service;

import com.sirh.model.dto.PointeuseDto;
import com.sirh.model.entity.Pointeuse;
import com.sirh.exception.PointeuseNotFoundException;
import com.sirh.model.repository.PointeuseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PointeuseService {

    private final PointeuseRepository pointeuseRepository;

    public PointeuseService(PointeuseRepository pointeuseRepository) {
        this.pointeuseRepository = pointeuseRepository;
    }

    @Transactional(readOnly = true)
    public List<PointeuseDto> findAll() {
        return pointeuseRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Pointeuse> findByAdresseIp(String adresseIp) {
        return pointeuseRepository.findByAdresseIpAndDeletedAtIsNull(adresseIp);
    }

    @Transactional
    public Pointeuse enregistrerSynchroByReference(UUID reference) {
        Pointeuse pointeuse = pointeuseRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new PointeuseNotFoundException(reference));
        pointeuse.setStatutEnLigne(true);
        pointeuse.setDerniereSynchro(LocalDateTime.now());
        return pointeuseRepository.save(pointeuse);
    }

    private PointeuseDto toDto(Pointeuse pointeuse) {
        return new PointeuseDto(
                pointeuse.getReference(),
                pointeuse.getNom(),
                pointeuse.getAdresseIp(),
                pointeuse.getPort(),
                pointeuse.getBatiment(),
                pointeuse.getStatutEnLigne(),
                pointeuse.getDerniereSynchro()
        );
    }
}
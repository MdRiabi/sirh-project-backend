package com.sirh.pointages.service;

import com.sirh.pointages.entity.Pointeuse;
import com.sirh.pointages.exception.PointeuseNotFoundException;
import com.sirh.pointages.repository.PointeuseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PointeuseService {

    private final PointeuseRepository pointeuseRepository;

    public PointeuseService(PointeuseRepository pointeuseRepository) {
        this.pointeuseRepository = pointeuseRepository;
    }

    @Transactional(readOnly = true)
    public List<Pointeuse> findAll() {
        return pointeuseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pointeuse> findByAdresseIp(String adresseIp) {
        return pointeuseRepository.findByAdresseIp(adresseIp);
    }

    @Transactional
    public Pointeuse enregistrerSynchro(Long pointeuseId) {
        Pointeuse pointeuse = pointeuseRepository.findById(pointeuseId)
                .orElseThrow(() -> new PointeuseNotFoundException(pointeuseId));
        pointeuse.setStatutEnLigne(true);
        pointeuse.setDerniereSynchro(LocalDateTime.now());
        return pointeuseRepository.save(pointeuse);
    }
}
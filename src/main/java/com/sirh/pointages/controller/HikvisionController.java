package com.sirh.pointages.controller;

import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.exception.EmployeNotFoundException;
import com.sirh.personnel.repository.EmployeRepository;
import com.sirh.pointages.dto.HikvisionEventRequest;
import com.sirh.pointages.entity.Pointeuse;
import com.sirh.pointages.entity.SourceImport;
import com.sirh.pointages.exception.PointeuseNotFoundException;
import com.sirh.pointages.service.PointageBrutService;
import com.sirh.pointages.service.PointeuseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dédié aux pointeuses physiques (mode Push, cahier-technique.md
 * §1.1) — pas de JWT, sécurisé en amont par HikvisionIpAllowlistFilter.
 */
@RestController
@RequestMapping("/api/v1/hikvision")
public class HikvisionController {

    private final PointeuseService pointeuseService;
    private final EmployeRepository employeRepository;
    private final PointageBrutService pointageBrutService;

    public HikvisionController(PointeuseService pointeuseService, EmployeRepository employeRepository,
                                PointageBrutService pointageBrutService) {
        this.pointeuseService = pointeuseService;
        this.employeRepository = employeRepository;
        this.pointageBrutService = pointageBrutService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> recevoirEvenement(@Valid @RequestBody HikvisionEventRequest request,
                                                    HttpServletRequest httpRequest) {
        Pointeuse pointeuse = pointeuseService.findByAdresseIp(httpRequest.getRemoteAddr())
                .orElseThrow(() -> new PointeuseNotFoundException(
                        "Pointeuse introuvable pour l'IP " + httpRequest.getRemoteAddr()));
        Employe employe = employeRepository.findByMatriculeInterne(request.badgeId())
                .orElseThrow(() -> new EmployeNotFoundException(
                        "Aucun employé pour le badgeId " + request.badgeId()));

        pointageBrutService.enregistrer(employe.getId(), pointeuse.getId(), request.timestamp(),
                request.typeEvenement(), request.methodeValidation(), SourceImport.HIKVISION_PUSH);
        pointeuseService.enregistrerSynchro(pointeuse.getId());

        return ResponseEntity.accepted().build();
    }
}
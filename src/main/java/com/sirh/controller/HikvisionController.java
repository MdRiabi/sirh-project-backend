package com.sirh.controller;

import com.sirh.model.entity.Employe;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.dto.HikvisionEventRequest;
import com.sirh.model.entity.Pointeuse;
import com.sirh.model.enums.SourceImport;
import com.sirh.service.PointageBrutService;
import com.sirh.service.PointeuseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class HikvisionController {

    private final PointeuseService pointeuseService;
    private final EmployeRepository employeRepository;
    private final PointageBrutService pointageBrutService;

    @PostMapping("/events")
    public ResponseEntity<Void> recevoirEvenement(@Valid @RequestBody HikvisionEventRequest request,
                                                    HttpServletRequest httpRequest) {
        Pointeuse pointeuse = pointeuseService.findByAdresseIp(httpRequest.getRemoteAddr())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POINTEUSE_NOT_FOUND_BY_IP,
                        new Object[]{httpRequest.getRemoteAddr()}));
        Employe employe = employeRepository.findByMatriculeInterneAndDeletedAtIsNull(request.badgeId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMPLOYE_NOT_FOUND_BY_BADGE,
                        new Object[]{request.badgeId()}));

        pointageBrutService.enregistrer(employe.getId(), pointeuse.getId(), request.timestamp(),
                request.typeEvenement(), request.methodeValidation(), SourceImport.HIKVISION_PUSH);
        pointeuseService.enregistrerSynchroByReference(pointeuse.getReference());

        return ResponseEntity.accepted().build();
    }
}
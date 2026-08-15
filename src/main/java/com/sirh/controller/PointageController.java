package com.sirh.controller;

import com.sirh.security.SirhUserDetails;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.dto.CorrectionPointageRequest;
import com.sirh.model.dto.PointageBrutDto;
import com.sirh.model.entity.PointageBrut;
import com.sirh.model.enums.PointageStatut;
import com.sirh.model.entity.Pointeuse;
import com.sirh.model.repository.PointeuseRepository;
import com.sirh.service.ImportPointagesService;
import com.sirh.service.PointageBrutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pointages")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class PointageController {

    private final PointageBrutService pointageBrutService;
    private final ImportPointagesService importPointagesService;
    private final EmployeRepository employeRepository;
    private final PointeuseRepository pointeuseRepository;

    public PointageController(PointageBrutService pointageBrutService, ImportPointagesService importPointagesService,
                               EmployeRepository employeRepository, PointeuseRepository pointeuseRepository) {
        this.pointageBrutService = pointageBrutService;
        this.importPointagesService = importPointagesService;
        this.employeRepository = employeRepository;
        this.pointeuseRepository = pointeuseRepository;
    }

    @GetMapping
    public ResponseEntity<List<PointageBrutDto>> list(
            @RequestParam(required = false) UUID employeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) PointageStatut statut) {
        List<PointageBrut> pointages = pointageBrutService.search(employeId, dateDebut, dateFin, statut);

        Map<UUID, Employe> employesParId = employeRepository
                .findAllById(pointages.stream().map(PointageBrut::getEmployeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employe::getId, Function.identity()));
        Map<Long, Pointeuse> pointeusesParId = pointeuseRepository
                .findAllById(pointages.stream().map(PointageBrut::getPointeuseId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Pointeuse::getId, Function.identity()));

        List<PointageBrutDto> dtos = pointages.stream()
                .map(p -> {
                    Employe employe = employesParId.get(p.getEmployeId());
                    Pointeuse pointeuse = p.getPointeuseId() != null ? pointeusesParId.get(p.getPointeuseId()) : null;
                    return PointageBrutDto.from(p,
                            employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            pointeuse != null ? pointeuse.getNom() : null);
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, UUID>> importer(@RequestParam("file") MultipartFile file,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        var job = importPointagesService.creerJob(file, principal.getUserId());
        return ResponseEntity.accepted().body(Map.of("jobId", job.getReference()));
    }

    @PutMapping("/{reference}/corriger")
    public ResponseEntity<PointageBrutDto> corrigerPointageByReference(@PathVariable UUID reference,
                                                      @Valid @RequestBody CorrectionPointageRequest request,
                                                      @AuthenticationPrincipal SirhUserDetails principal,
                                                      HttpServletRequest httpRequest) {
        var pointage = pointageBrutService.corrigerPointageByReference(reference, request, principal.getUserId(), httpRequest.getRemoteAddr());
        Employe employe = employeRepository.findById(pointage.getEmployeId()).orElse(null);
        Pointeuse pointeuse = pointage.getPointeuseId() != null
                ? pointeuseRepository.findById(pointage.getPointeuseId()).orElse(null) : null;
        return ResponseEntity.ok(PointageBrutDto.from(pointage,
                employe != null ? employe.getNom() : null,
                employe != null ? employe.getPrenom() : null,
                pointeuse != null ? pointeuse.getNom() : null));
    }
}
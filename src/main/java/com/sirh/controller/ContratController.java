package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.ContratDto;
import com.sirh.model.dto.ContratRequest;
import com.sirh.service.ContratService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employes/{employeId}/contrats")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class ContratController {

    private final ContratService contratService;

    public ContratController(ContratService contratService) {
        this.contratService = contratService;
    }

    @GetMapping
    public ResponseEntity<ListingPageDto<ContratDto>> list(@PathVariable UUID employeId) {
        return ResponseEntity.ok(ListingPageDto.of(contratService.findByEmploye(employeId)));
    }

    @PostMapping
    public ResponseEntity<SingleObjectDto<ContratDto>> create(@PathVariable UUID employeId,
                                              @Valid @RequestBody ContratRequest request,
                                              @AuthenticationPrincipal SirhUserDetails principal) {
        var contrat = contratService.create(employeId, request, principal.getUserId());
        return ResponseEntity.status(201).body(SingleObjectDto.of(contrat));
    }
}

package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.DepartementDto;
import com.sirh.model.dto.DepartementRequest;
import com.sirh.service.DepartementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/departements")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class DepartementController {

  private final DepartementService departementService;

  @GetMapping
  public ResponseEntity<ListingPageDto<DepartementDto>> list() {
    return ResponseEntity.ok(ListingPageDto.of(departementService.findAll()));
  }

  @GetMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<DepartementDto>> getDepartementByReference(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(departementService.getDepartementByReference(reference)));
  }

  @PostMapping
  public ResponseEntity<SingleObjectDto<DepartementDto>> create(
      @Valid @RequestBody DepartementRequest request) {
    return ResponseEntity.status(201).body(SingleObjectDto.of(departementService.create(request)));
  }

  @PutMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<DepartementDto>> updateDepartementByReference(
      @PathVariable UUID reference, @Valid @RequestBody DepartementRequest request) {
    return ResponseEntity.ok(
        SingleObjectDto.of(departementService.updateDepartementByReference(reference, request)));
  }
}

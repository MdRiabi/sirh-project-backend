package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.ShiftDto;
import com.sirh.model.dto.ShiftRequest;
import com.sirh.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shifts")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER', 'ROLE_MANAGER')")
@RequiredArgsConstructor
public class ShiftController {

  private final ShiftService shiftService;

  @GetMapping
  public ResponseEntity<ListingPageDto<ShiftDto>> list() {
    return ResponseEntity.ok(ListingPageDto.of(shiftService.findAll()));
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
  public ResponseEntity<SingleObjectDto<ShiftDto>> create(
      @Valid @RequestBody ShiftRequest request) {
    return ResponseEntity.status(201).body(SingleObjectDto.of(shiftService.createShift(request)));
  }
}

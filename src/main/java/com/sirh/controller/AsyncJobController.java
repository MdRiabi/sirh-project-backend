package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.AsyncJobDto;
import com.sirh.model.enums.AsyncJobType;
import com.sirh.service.AsyncJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Jobs asynchrones",
    description =
        "Polling générique des `async_jobs` (import pointages, génération de rapports, etc.)")
@RestController
@RequestMapping("/async-jobs")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
@RequiredArgsConstructor
public class AsyncJobController {

  private final AsyncJobService asyncJobService;

  @Operation(
      summary = "Lister les jobs asynchrones par type",
      description = "Ex. `type=IMPORT_POINTAGES` ou `type=REPORT`.")
  @GetMapping
  public ResponseEntity<ListingPageDto<AsyncJobDto>> list(
      @Parameter(description = "Type de job à filtrer") @RequestParam AsyncJobType type) {
    return ResponseEntity.ok(ListingPageDto.of(asyncJobService.findByType(type)));
  }

  @Operation(summary = "Consulter l'état d'un job asynchrone")
  @GetMapping("/{reference}")
  public ResponseEntity<SingleObjectDto<AsyncJobDto>> getAsyncJobByReference(
      @PathVariable UUID reference) {
    return ResponseEntity.ok(
        SingleObjectDto.of(asyncJobService.getAsyncJobDtoByReference(reference)));
  }
}

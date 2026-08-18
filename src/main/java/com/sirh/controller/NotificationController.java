package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.NotificationDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Notifications",
    description =
        "Notifications de l'utilisateur authentifié (`DEMANDE_A_VALIDER`, `ALERTE_POINTAGE`,"
            + " `ALERTE_SOUS_ACTIVITE`) — tout rôle authentifié, scope limité à ses propres"
            + " notifications. Pas de POST — générées uniquement côté serveur (événements"
            + " métier, jobs planifiés), jamais par un appel client.")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "Lister mes notifications")
  @GetMapping
  public ResponseEntity<ListingPageDto<NotificationDto>> list(
      @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        ListingPageDto.of(notificationService.mesNotifications(principal.getUserId())));
  }

  @Operation(summary = "Marquer une notification comme lue")
  @PutMapping("/{reference}/lue")
  public ResponseEntity<SingleObjectDto<NotificationDto>> marquerNotificationLueByReference(
      @PathVariable UUID reference, @AuthenticationPrincipal SirhUserDetails principal) {
    return ResponseEntity.ok(
        SingleObjectDto.of(
            notificationService.marquerNotificationLueByReference(
                reference, principal.getUserId())));
  }
}

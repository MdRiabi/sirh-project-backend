package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.enums.Role;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.CheckInRequest;
import com.sirh.model.dto.CheckOutRequest;
import com.sirh.model.dto.OutillageCreateRequest;
import com.sirh.model.dto.OutillageDto;
import com.sirh.model.dto.OutillageMouvementDto;
import com.sirh.model.entity.Outillage;
import com.sirh.service.OutillageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Gestion du matériel (cahier-fonctionnel.md Module 5) : rattachement/
// restitution exclusifs au Responsable Département de l'outil ("gestion
// exclusivement effectuée par le Responsable Département") — RH/SUPERUSER
// restent en lecture seule (visibilité pour le traitement financier des
// pertes déclarées, GET /outillages, GET /outillages/{id}/historique).
@RestController
@RequestMapping("/outillages")
public class OutillageController {

    private final OutillageService outillageService;

    public OutillageController(OutillageService outillageService) {
        this.outillageService = outillageService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<SingleObjectDto<OutillageDto>> creer(@Valid @RequestBody OutillageCreateRequest request,
                                               @AuthenticationPrincipal SirhUserDetails principal) {
        Long departementId = principal.getDepartementId();
        if (departementId == null) {
            throw new AccessDeniedException("Aucun département assigné à ce compte : impossible de créer un outil.");
        }
        OutillageDto outillage = outillageService.creer(request.codeBarres(), request.nomOutil(), request.valeurAchat(),
                request.dateAchat(), departementId);
        return ResponseEntity.status(201).body(SingleObjectDto.of(outillage));
    }

    // MANAGER restreint à son propre département (departementId client ignoré),
    // même convention que GET /autorisations, GET /assiduite/calcul-mensuel —
    // 🔧 mais un Manager sans département assigné doit voir une liste vide,
    // pas tout le parc (departementEffectif = null tomberait sinon sur
    // findAll(), indistinguable du cas RH/SUPERUSER "aucun filtre" = tout
    // voir ; trouvé en revue de code, même risque que
    // AssiduiteController.calculMensuel, hors périmètre Lot 7).
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ListingPageDto<OutillageDto>> lister(@RequestParam(required = false) Long departementId,
                                                       @AuthenticationPrincipal SirhUserDetails principal) {
        List<OutillageDto> outillages;
        if (principal.getRole() == Role.ROLE_MANAGER) {
            Long departementManager = principal.getDepartementId();
            outillages = departementManager != null ? outillageService.findByDepartement(departementManager) : List.of();
        } else {
            outillages = departementId != null ? outillageService.findByDepartement(departementId) : outillageService.findAll();
        }
        return ResponseEntity.ok(ListingPageDto.of(outillages));
    }

    @PostMapping("/{reference}/check-out")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<SingleObjectDto<OutillageMouvementDto>> checkOut(@PathVariable UUID reference,
                                                            @Valid @RequestBody CheckOutRequest request,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        Outillage outillage = outillageService.getOutillageByReference(reference);
        verifierScopeManager(principal, outillage);
        var mouvement = outillageService.checkOut(outillage.getId(), request.employeId(), principal.getUserId());
        return ResponseEntity.status(201).body(SingleObjectDto.of(mouvement));
    }

    @PostMapping("/{reference}/check-in")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<SingleObjectDto<OutillageMouvementDto>> checkIn(@PathVariable UUID reference,
                                                           @Valid @RequestBody CheckInRequest request,
                                                           @AuthenticationPrincipal SirhUserDetails principal) {
        Outillage outillage = outillageService.getOutillageByReference(reference);
        verifierScopeManager(principal, outillage);
        var mouvement = outillageService.checkIn(outillage.getId(), request.etat(), request.commentaire(), principal.getUserId());
        return ResponseEntity.status(201).body(SingleObjectDto.of(mouvement));
    }

    @GetMapping("/{reference}/historique")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<ListingPageDto<OutillageMouvementDto>> historique(@PathVariable UUID reference,
                                                                    @AuthenticationPrincipal SirhUserDetails principal) {
        Outillage outillage = outillageService.getOutillageByReference(reference);
        if (principal.getRole() == Role.ROLE_MANAGER) {
            verifierScopeManager(principal, outillage);
        }
        return ResponseEntity.ok(ListingPageDto.of(outillageService.historique(outillage.getId())));
    }

    private void verifierScopeManager(SirhUserDetails principal, Outillage outillage) {
        if (!Objects.equals(principal.getDepartementId(), outillage.getDepartementId())) {
            throw new AccessDeniedException("Accès limité au matériel de son propre département.");
        }
    }
}

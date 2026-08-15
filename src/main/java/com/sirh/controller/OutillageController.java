package com.sirh.controller;

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
    public ResponseEntity<OutillageDto> creer(@Valid @RequestBody OutillageCreateRequest request,
                                               @AuthenticationPrincipal SirhUserDetails principal) {
        Long departementId = principal.getDepartementId();
        if (departementId == null) {
            throw new AccessDeniedException("Aucun département assigné à ce compte : impossible de créer un outil.");
        }
        Outillage outillage = outillageService.creer(request.codeBarres(), request.nomOutil(), request.valeurAchat(),
                request.dateAchat(), departementId);
        return ResponseEntity.status(201).body(outillageService.enrichir(List.of(outillage)).get(0));
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
    public ResponseEntity<List<OutillageDto>> lister(@RequestParam(required = false) Long departementId,
                                                       @AuthenticationPrincipal SirhUserDetails principal) {
        List<Outillage> outillages;
        if (principal.getRole() == Role.ROLE_MANAGER) {
            Long departementManager = principal.getDepartementId();
            outillages = departementManager != null ? outillageService.findByDepartement(departementManager) : List.of();
        } else {
            outillages = departementId != null ? outillageService.findByDepartement(departementId) : outillageService.findAll();
        }
        return ResponseEntity.ok(outillageService.enrichir(outillages));
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<OutillageMouvementDto> checkOut(@PathVariable Long id,
                                                            @Valid @RequestBody CheckOutRequest request,
                                                            @AuthenticationPrincipal SirhUserDetails principal) {
        verifierScopeManager(principal, outillageService.findById(id));
        var mouvement = outillageService.checkOut(id, request.employeId(), principal.getUserId());
        return ResponseEntity.status(201).body(OutillageMouvementDto.from(mouvement));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<OutillageMouvementDto> checkIn(@PathVariable Long id,
                                                           @Valid @RequestBody CheckInRequest request,
                                                           @AuthenticationPrincipal SirhUserDetails principal) {
        verifierScopeManager(principal, outillageService.findById(id));
        var mouvement = outillageService.checkIn(id, request.etat(), request.commentaire(), principal.getUserId());
        return ResponseEntity.status(201).body(OutillageMouvementDto.from(mouvement));
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_RH', 'ROLE_SUPERUSER')")
    public ResponseEntity<List<OutillageMouvementDto>> historique(@PathVariable Long id,
                                                                    @AuthenticationPrincipal SirhUserDetails principal) {
        Outillage outillage = outillageService.findById(id);
        if (principal.getRole() == Role.ROLE_MANAGER) {
            verifierScopeManager(principal, outillage);
        }
        return ResponseEntity.ok(outillageService.historique(id).stream().map(OutillageMouvementDto::from).toList());
    }

    private void verifierScopeManager(SirhUserDetails principal, Outillage outillage) {
        if (!Objects.equals(principal.getDepartementId(), outillage.getDepartementId())) {
            throw new AccessDeniedException("Accès limité au matériel de son propre département.");
        }
    }
}

package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.model.dto.OutillageDto;
import com.sirh.model.dto.OutillageMouvementDto;
import com.sirh.model.enums.EtatMouvement;
import com.sirh.model.entity.Outillage;
import com.sirh.model.enums.OutillageEtat;
import com.sirh.model.entity.OutillageMouvement;
import com.sirh.model.enums.TypeMouvement;
import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.repository.OutillageMouvementRepository;
import com.sirh.model.repository.OutillageRepository;
import com.sirh.service.ParametreGlobalService;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// Gestion du matériel (cahier-fonctionnel.md Module 5) : rattachement/
// restitution exclusifs au Responsable Département de l'outil, historique
// intégral en outillage_mouvements (constat 10, cahier-technique.md §3.14),
// barème d'imputation financière appliqué uniquement en cas de perte
// déclarée au check-in (cf. Module 5, "Perte & endommagement").
@Service
public class OutillageService {

    private static final String CLE_TAUX_0_6 = "TAUX_IMPUTATION_OUTIL_0_6_MOIS";
    private static final String CLE_TAUX_6_12 = "TAUX_IMPUTATION_OUTIL_6_12_MOIS";
    private static final String CLE_TAUX_PLUS_12 = "TAUX_IMPUTATION_OUTIL_PLUS_12_MOIS";

    private final OutillageRepository outillageRepository;
    private final OutillageMouvementRepository outillageMouvementRepository;
    private final EmployeRepository employeRepository;
    private final ParametreGlobalService parametreGlobalService;

    public OutillageService(OutillageRepository outillageRepository,
                             OutillageMouvementRepository outillageMouvementRepository,
                             EmployeRepository employeRepository, ParametreGlobalService parametreGlobalService) {
        this.outillageRepository = outillageRepository;
        this.outillageMouvementRepository = outillageMouvementRepository;
        this.employeRepository = employeRepository;
        this.parametreGlobalService = parametreGlobalService;
    }

    @Transactional
    public OutillageDto creer(String codeBarres, String nomOutil, BigDecimal valeurAchat, LocalDate dateAchat,
                            Long departementId) {
        Outillage outillage = Outillage.builder()
                .codeBarres(codeBarres)
                .nomOutil(nomOutil)
                .etat(OutillageEtat.BON_ETAT)
                .valeurAchat(valeurAchat)
                .dateAchat(dateAchat)
                .departementId(departementId)
                .build();
        return enrichir(List.of(outillageRepository.save(outillage))).get(0);
    }

    @Transactional(readOnly = true)
    public Outillage getOutillageByReference(UUID reference) {
        return outillageRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OUTILLAGE_NOT_FOUND, new Object[]{reference}));
    }

    private Outillage getOutillageById(Long id) {
        return outillageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OUTILLAGE_NOT_FOUND, new Object[]{id}));
    }

    @Transactional(readOnly = true)
    public List<OutillageDto> findByDepartement(Long departementId) {
        return enrichir(outillageRepository.findByDepartementIdAndDeletedAtIsNull(departementId));
    }

    @Transactional(readOnly = true)
    public List<OutillageDto> findAll() {
        return enrichir(outillageRepository.findAll());
    }

    @Audited(action = "CHECK_OUT_OUTILLAGE", tableCible = "outillage_mouvements")
    @Transactional
    public OutillageMouvementDto checkOut(Long outillageId, UUID employeId, Long effectueParId) {
        Outillage outillage = getOutillageById(outillageId);
        if (outillage.getAttribueAEmployeId() != null) {
            throw new ConflictException(ErrorCode.OUTILLAGE_DEJA_ATTRIBUE);
        }
        Employe employe = employeRepository.findById(employeId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMPLOYE_NOT_FOUND, new Object[]{employeId}));
        if (!Objects.equals(employe.getDepartementId(), outillage.getDepartementId())) {
            throw new AccessDeniedException("L'employé doit appartenir au même département que l'outil.");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        outillage.setAttribueAEmployeId(employeId);
        outillage.setDateAttribution(maintenant);
        outillage.setEtat(OutillageEtat.ATTRIBUE);
        outillageRepository.save(outillage);

        OutillageMouvement mouvement = OutillageMouvement.builder()
                .outillageId(outillageId)
                .employeId(employeId)
                .typeMouvement(TypeMouvement.CHECK_OUT)
                .effectueParId(effectueParId)
                .build();
        return toDto(outillageMouvementRepository.save(mouvement));
    }

    @Audited(action = "CHECK_IN_OUTILLAGE", tableCible = "outillage_mouvements")
    @Transactional
    public OutillageMouvementDto checkIn(Long outillageId, EtatMouvement etat, String commentaire, Long effectueParId) {
        Outillage outillage = getOutillageById(outillageId);
        if (outillage.getAttribueAEmployeId() == null) {
            throw new ConflictException(ErrorCode.OUTILLAGE_NON_ATTRIBUE);
        }
        UUID employeId = outillage.getAttribueAEmployeId();

        BigDecimal montantImputation = etat == EtatMouvement.PERDU
                ? calculerMontantImputation(outillage.getValeurAchat(), outillage.getDateAchat())
                : BigDecimal.ZERO;

        outillage.setAttribueAEmployeId(null);
        outillage.setDateAttribution(null);
        outillage.setEtat(etatOutillageApresCheckIn(etat));
        outillageRepository.save(outillage);

        OutillageMouvement mouvement = OutillageMouvement.builder()
                .outillageId(outillageId)
                .employeId(employeId)
                .typeMouvement(TypeMouvement.CHECK_IN)
                .etat(etat)
                .montantImputation(montantImputation)
                .commentaire(commentaire)
                .effectueParId(effectueParId)
                .build();
        return toDto(outillageMouvementRepository.save(mouvement));
    }

    private static OutillageEtat etatOutillageApresCheckIn(EtatMouvement etat) {
        return switch (etat) {
            case BON -> OutillageEtat.BON_ETAT;
            case USE -> OutillageEtat.USE;
            case CASSE -> OutillageEtat.CASSE;
            case PERDU -> OutillageEtat.PERDU;
        };
    }

    // Barème par ancienneté du matériel depuis son achat (Module 5, "traitement
    // financier"), pas depuis la dernière attribution — un outil réattribué
    // plusieurs fois reste "le même outil" au sens comptable. Valeurs par
    // défaut 100/50/25% relues à chaque appel (parametres_globaux, jamais de
    // cache), même principe que les seuils du Lot 6.
    private BigDecimal calculerMontantImputation(BigDecimal valeurAchat, LocalDate dateAchat) {
        long anncienneteMois = ChronoUnit.MONTHS.between(dateAchat, LocalDate.now());
        String cleTaux = anncienneteMois < 6 ? CLE_TAUX_0_6 : anncienneteMois < 12 ? CLE_TAUX_6_12 : CLE_TAUX_PLUS_12;
        BigDecimal taux = parametreGlobalService.valeurDecimale(cleTaux);
        return valeurAchat.multiply(taux)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<OutillageMouvementDto> historique(Long outillageId) {
        getOutillageById(outillageId);
        return outillageMouvementRepository.findByOutillageIdAndDeletedAtIsNullOrderByCreatedAtDesc(outillageId)
                .stream().map(this::toDto).toList();
    }

    private List<OutillageDto> enrichir(List<Outillage> outillages) {
        List<UUID> employeIds = outillages.stream()
                .map(Outillage::getAttribueAEmployeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Employe> employesParId = employeRepository.findAllById(employeIds).stream()
                .collect(Collectors.toMap(Employe::getId, e -> e));

        return outillages.stream()
                .map(o -> {
                    Employe employe = employesParId.get(o.getAttribueAEmployeId());
                    return new OutillageDto(o.getReference(), o.getCodeBarres(), o.getNomOutil(),
                            o.getEtat(), o.getAttribueAEmployeId(),
                            employe != null ? employe.getNom() : null,
                            employe != null ? employe.getPrenom() : null,
                            o.getDateAttribution(), o.getDepartementId(), o.getValeurAchat(), o.getDateAchat());
                })
                .toList();
    }

    private OutillageMouvementDto toDto(OutillageMouvement mouvement) {
        return new OutillageMouvementDto(mouvement.getReference(), mouvement.getOutillageId(), mouvement.getEmployeId(),
                mouvement.getTypeMouvement(), mouvement.getEtat(), mouvement.getMontantImputation(),
                mouvement.getCommentaire(), mouvement.getEffectueParId(), mouvement.getCreatedAt());
    }
}

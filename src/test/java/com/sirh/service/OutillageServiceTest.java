package com.sirh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sirh.config.ModelMapperConfig;
import com.sirh.exception.ConflictException;
import com.sirh.model.entity.Departement;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Outillage;
import com.sirh.model.enums.EtatMouvement;
import com.sirh.model.enums.OutillageEtat;
import com.sirh.model.enums.TypeMouvement;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.OutillageMouvementRepository;
import com.sirh.model.repository.OutillageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Couvre le workflow rattachement/restitution (Module 5, Lot 7) : refus des transitions invalides
 * (double check-out, check-in sans attribution), restriction au département de l'outil, et le
 * barème d'imputation par tranche d'ancienneté (0-6/6-12/>12 mois) — les taux viennent de
 * parametres_globaux, jamais codés en dur (cf. AssiduiteCalculServiceTest, même convention).
 */
@ExtendWith(MockitoExtension.class)
class OutillageServiceTest {

  private static final Long DEPARTEMENT_ID = 1L;

  @Mock private OutillageRepository outillageRepository;
  @Mock private OutillageMouvementRepository outillageMouvementRepository;
  @Mock private EmployeRepository employeRepository;
  @Mock private ParametreGlobalService parametreGlobalService;

  private OutillageService service;

  @BeforeEach
  void setUp() {
    service =
        new OutillageService(
            outillageRepository,
            outillageMouvementRepository,
            employeRepository,
            parametreGlobalService,
            new ModelMapperConfig().modelMapper());
    lenient()
        .when(outillageRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(outillageMouvementRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Outillage outillage(BigDecimal valeurAchat, LocalDate dateAchat) {
    return Outillage.builder()
        .id(1L)
        .codeBarres("BC-1")
        .nomOutil("Perceuse")
        .etat(OutillageEtat.BON_ETAT)
        .departementId(DEPARTEMENT_ID)
        .valeurAchat(valeurAchat)
        .dateAchat(dateAchat)
        .build();
  }

  private Employe employe(Long departementId) {
    return Employe.builder()
        .id(UUID.randomUUID())
        .nom("Ali")
        .prenom("Ben")
        .departement(Departement.builder().id(departementId).build())
        .build();
  }

  @Test
  void checkOut_outilDisponible_attribueEtEnregistreLeMouvement() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now());
    Employe employe = employe(DEPARTEMENT_ID);
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));
    when(employeRepository.findById(employe.getId())).thenReturn(Optional.of(employe));

    var mouvement = service.checkOut(1L, employe.getId(), 99L);

    assertThat(outil.getEtat()).isEqualTo(OutillageEtat.ATTRIBUE);
    assertThat(outil.getAttribueAEmployeId()).isEqualTo(employe.getId());
    assertThat(mouvement.getTypeMouvement()).isEqualTo(TypeMouvement.CHECK_OUT);
    assertThat(mouvement.getEffectueParId()).isEqualTo(99L);
  }

  @Test
  void checkOut_outilDejaAttribue_estRefuse() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now());
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));

    assertThatThrownBy(() -> service.checkOut(1L, UUID.randomUUID(), 99L))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void checkOut_employeHorsDuDepartementDeLOutil_estRefuse() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now());
    Employe employeAutreDepartement = employe(2L);
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));
    when(employeRepository.findById(employeAutreDepartement.getId()))
        .thenReturn(Optional.of(employeAutreDepartement));

    assertThatThrownBy(() -> service.checkOut(1L, employeAutreDepartement.getId(), 99L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void checkIn_outilNonAttribue_estRefuse() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));

    assertThatThrownBy(() -> service.checkIn(1L, EtatMouvement.BON, null, 99L))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void checkIn_etatBon_libereLOutilEtNeCalculeAucuneImputation() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now());
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));

    var mouvement = service.checkIn(1L, EtatMouvement.BON, "RAS", 99L);

    assertThat(outil.getEtat()).isEqualTo(OutillageEtat.BON_ETAT);
    assertThat(outil.getAttribueAEmployeId()).isNull();
    assertThat(mouvement.getMontantImputation()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void checkIn_perteMoinsDe6Mois_appliqueLeTauxLePlusEleve() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now().minusMonths(3));
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));
    when(parametreGlobalService.valeurDecimale("TAUX_IMPUTATION_OUTIL_0_6_MOIS"))
        .thenReturn(BigDecimal.valueOf(100));

    var mouvement = service.checkIn(1L, EtatMouvement.PERDU, null, 99L);

    assertThat(outil.getEtat()).isEqualTo(OutillageEtat.PERDU);
    assertThat(mouvement.getMontantImputation()).isEqualByComparingTo("1000.00");
  }

  @Test
  void checkIn_perteEntre6Et12Mois_appliqueLeTauxIntermediaire() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now().minusMonths(8));
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));
    when(parametreGlobalService.valeurDecimale("TAUX_IMPUTATION_OUTIL_6_12_MOIS"))
        .thenReturn(BigDecimal.valueOf(50));

    var mouvement = service.checkIn(1L, EtatMouvement.PERDU, null, 99L);

    assertThat(mouvement.getMontantImputation()).isEqualByComparingTo("500.00");
  }

  @Test
  void checkIn_pertePlusDe12Mois_appliqueLeTauxLePlusFaible() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now().minusMonths(20));
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));
    when(parametreGlobalService.valeurDecimale("TAUX_IMPUTATION_OUTIL_PLUS_12_MOIS"))
        .thenReturn(BigDecimal.valueOf(25));

    var mouvement = service.checkIn(1L, EtatMouvement.PERDU, null, 99L);

    assertThat(mouvement.getMontantImputation()).isEqualByComparingTo("250.00");
  }

  @Test
  void checkIn_casse_neCalculeAucuneImputationMaisMetLOutilEnEtatCasse() {
    Outillage outil = outillage(BigDecimal.valueOf(1000), LocalDate.now().minusMonths(20));
    outil.setAttribueAEmployeId(UUID.randomUUID());
    when(outillageRepository.findById(1L)).thenReturn(Optional.of(outil));

    var mouvement = service.checkIn(1L, EtatMouvement.CASSE, "Chute", 99L);

    assertThat(outil.getEtat()).isEqualTo(OutillageEtat.CASSE);
    assertThat(mouvement.getMontantImputation()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}

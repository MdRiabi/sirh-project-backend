package com.sirh.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sirh.model.dto.ContratDto;
import com.sirh.model.entity.Contrat;
import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.enums.MotifFin;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

/**
 * Filet de sécurité pour le mode STRICT : ModelMapper ne mappe un champ que si les noms se
 * correspondent exactement des deux côtés — une divergence de nom (ex. renommage d'un champ dans
 * Contrat ou ContratDto) fait échouer ce test plutôt que de silencieusement laisser le champ à sa
 * valeur par défaut.
 */
class ModelMapperConfigTest {

  private final ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

  @Test
  void mapsContratToContratDtoByMatchingFieldNames() {
    Contrat contrat =
        Contrat.builder()
            .employeId(UUID.randomUUID())
            .type(ContratType.CDI)
            .dateDebut(LocalDate.of(2026, 1, 1))
            .dateFin(LocalDate.of(2026, 12, 31))
            .statut(ContratStatut.EN_COURS)
            .motifFin(MotifFin.FIN_CDD)
            .build();
    contrat.setReference(UUID.randomUUID());
    contrat.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));

    ContratDto dto = modelMapper.map(contrat, ContratDto.class);

    assertEquals(contrat.getReference(), dto.getReference());
    assertEquals(contrat.getEmployeId(), dto.getEmployeId());
    assertEquals(contrat.getType(), dto.getType());
    assertEquals(contrat.getDateDebut(), dto.getDateDebut());
    assertEquals(contrat.getDateFin(), dto.getDateFin());
    assertEquals(contrat.getStatut(), dto.getStatut());
    assertEquals(contrat.getMotifFin(), dto.getMotifFin());
    assertEquals(contrat.getCreatedAt(), dto.getCreatedAt());
  }
}

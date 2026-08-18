package com.sirh.model.dto;

import com.sirh.model.enums.ContratStatut;
import com.sirh.model.enums.ContratType;
import com.sirh.model.enums.MotifFin;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContratDto {
  private UUID reference;
  private UUID employeId;
  private ContratType type;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private ContratStatut statut;
  private MotifFin motifFin;
  private LocalDateTime createdAt;
}

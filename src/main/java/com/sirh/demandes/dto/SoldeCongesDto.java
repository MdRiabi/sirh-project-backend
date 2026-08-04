package com.sirh.demandes.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SoldeCongesDto(UUID employeId, BigDecimal soldeCongesJours) {
}

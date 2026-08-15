package com.sirh.event;

import java.util.UUID;

/**
 * Déclencheur Module 11 (DEMANDE_A_VALIDER) : publié par AutorisationService/
 * IndisponibiliteService (Lot 5) uniquement pour le workflow Manager normal
 * (statut EN_ATTENTE_RH) — le workflow d'urgence RH est déjà VALIDE à la
 * création, rien à notifier. typeDemande/demandeId pointent vers autorisations
 * ou indisponibilites (id Long), jamais vers employes (id UUID).
 */
public record DemandeCreeEvent(String typeDemande, Long demandeId, UUID employeId, String libelle) {
}

-- ============================================================================
-- V5 — Shift "système" (Lot 5) : shifts.heure_debut/heure_fin sont NOT NULL
-- mais purement catégoriels (PlanningService n'utilise jamais ces bornes pour
-- calculer des heures, seules Planning.date_debut/date_fin comptent). Ce shift
-- sert donc de support générique unique aux créneaux CONGE/ABSENCE générés
-- automatiquement à la validation d'une demande (com.sirh.demandes), qui
-- n'ont pas de créneau horaire réel à référencer.
-- ============================================================================

INSERT INTO shifts (nom, type_rotation, heure_debut, heure_fin)
VALUES ('Congé / Absence (généré automatiquement)', 'SYSTEME', '00:00:00', '23:59:59');

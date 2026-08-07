-- ============================================================================
-- V8 — Lot 8 (Module 11 : Gestion des Notifications). Table `notifications`
-- déjà créée par V1__init_schema.sql (schéma consolidé initial, §18) avec
-- exactement la structure du cahier-technique.md §3.18 — rien à migrer côté
-- table, seuls les paramètres métier du module manquent encore.
--
-- Deux seuils explicitement chiffrés au CDF (3 oublis de badgeage/semaine,
-- ≤5 jours travaillés/30 jours) — même principe que TAUX_INCREMENTATION_
-- CONGES_MENSUEL (Lot 5) et les seuils du Lot 6/7 : jamais en dur, modifiables
-- par le RH sans redéploiement via PUT /parametres-globaux/{cle}.
--
-- TYPES_SUSPENSION_LEGALE comble un gap : le CDF cite "congé maternité ou
-- arrêt maladie longue durée" (Module 11, faux positif sous-activité) comme
-- motifs de suspension légale de contrat, mais indisponibilites.type_
-- indisponibilite est un VARCHAR libre sans enum applicatif (cf. commentaire
-- Autorisation.java) — aucune liste fermée de types n'existe en base. Liste
-- CSV dynamique plutôt qu'un type figé en dur dans le code Java.
-- ============================================================================

INSERT INTO parametres_globaux (cle, valeur, type, description) VALUES
    ('SEUIL_OUBLIS_BADGEAGE_HEBDO', '3', 'INTEGER',
     'Nombre d''oublis de badgeage (anomalies non justifiées) dans la semaine à partir duquel une alerte est envoyée au Manager direct (Module 11).'),
    ('SEUIL_JOURS_TRAVAILLES_SOUS_ACTIVITE', '5', 'INTEGER',
     'Nombre de jours travaillés sur les 30 derniers jours en dessous (ou égal) duquel une alerte de sous-activité est envoyée au RH pour un contrat actif (Module 11).'),
    ('TYPES_SUSPENSION_LEGALE', 'MALADIE,MATERNITE,PATERNITE,ACCIDENT_TRAVAIL', 'LISTE',
     'Types d''indisponibilite (CSV) considérés comme suspension légale de contrat, exclus du calcul de l''alerte de sous-activité (Module 11, faux positif congé maternité/arrêt maladie longue durée).');

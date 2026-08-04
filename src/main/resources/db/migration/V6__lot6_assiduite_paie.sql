-- ============================================================================
-- V6 — Lot 6 (Module 3 : Assiduité & Paie). Aucune table de verrouillage
-- mensuel / demande de déverrouillage n'existait dans le schéma (gap trouvé
-- en préparant le lot, même nature que jours_feries au Lot 4) — cf. décision
-- d'architecture validée : clôture globale par mois (pas par département),
-- pas de table de snapshot des résultats (GET /assiduite/calcul-mensuel
-- recalcule à la volée, les données sources devenant read-only une fois le
-- mois clôturé).
-- ============================================================================

CREATE TABLE cloture_paie (
    id BIGSERIAL PRIMARY KEY,
    mois DATE NOT NULL UNIQUE,              -- toujours le 1er du mois (ex: 2026-07-01)
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERT', -- OUVERT, CLOTURE
    date_cloture TIMESTAMP,
    cloture_par_id UUID REFERENCES employes(id),
    taux_anomalies_justifiees NUMERIC(5,2), -- snapshot du contrôle au moment de la clôture
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demandes_deverrouillage (
    id BIGSERIAL PRIMARY KEY,
    mois DATE NOT NULL,
    motif TEXT NOT NULL,
    demande_par_id UUID NOT NULL REFERENCES employes(id),
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, APPROUVEE, REFUSEE
    traite_par_id UUID REFERENCES employes(id),
    date_traitement TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_deverrouillage_mois ON demandes_deverrouillage(mois, statut);

-- Seuils du moteur de calcul (Module 3) : aucune valeur chiffrée dans le CDF/
-- CDC-T, mêmes principe que TAUX_INCREMENTATION_CONGES_MENSUEL (Lot 5) —
-- valeurs par défaut raisonnables, modifiables par le RH sans redéploiement
-- via PUT /parametres-globaux/{cle}.
INSERT INTO parametres_globaux (cle, valeur, type, description) VALUES
    ('HEURE_DEBUT_NUIT', '21:00', 'TEXT',
     'Début de la plage horaire comptabilisée en heures de nuit (Module 3).'),
    ('HEURE_FIN_NUIT', '06:00', 'TEXT',
     'Fin de la plage horaire comptabilisée en heures de nuit (Module 3).'),
    ('SEUIL_HEURES_MAJOREES_JOUR', '8', 'INTEGER',
     'Nombre d''heures travaillées par jour au-delà duquel le surplus est comptabilisé en heures majorées (Module 3).'),
    ('SEUIL_PRIME_100', '0', 'INTEGER',
     'Nombre maximal d''anomalies de pointage non justifiées sur le mois pour conserver une prime d''assiduité à 100%.'),
    ('SEUIL_PRIME_50', '2', 'INTEGER',
     'Au-delà de SEUIL_PRIME_100 et jusqu''à ce seuil d''anomalies inclus, la prime d''assiduité tombe à 50%.'),
    ('SEUIL_PRIME_33', '4', 'INTEGER',
     'Au-delà de SEUIL_PRIME_50 et jusqu''à ce seuil d''anomalies inclus, la prime d''assiduité tombe à 33%. Au-delà, 25%.'),
    ('SEUIL_ANOMALIES_CLOTURE', '98', 'DECIMAL',
     'Pourcentage minimal d''anomalies de pointage justifiées sur le mois requis pour autoriser la clôture de paie (POST /assiduite/cloturer-paie).');

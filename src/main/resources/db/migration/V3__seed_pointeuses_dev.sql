-- ============================================================================
-- V3 — Pointeuses de test pour environnement de développement (Lot 2)
-- Une pointeuse en loopback (127.0.0.1) pour permettre de tester le webhook
-- Hikvision via `docker exec sirh-backend curl ...` (le filtre de liste
-- blanche IP compare l'IP source réelle de la requête). Ne jamais appliquer
-- cette migration sur un environnement de production.
-- ============================================================================

INSERT INTO pointeuses (nom, adresse_ip, port, batiment, statut_en_ligne)
VALUES
    ('Pointeuse Test (loopback)', '127.0.0.1', 80, 'Batiment A', TRUE),
    ('Entrée Bâtiment B', '10.10.2.15', 80, 'Batiment B', FALSE);
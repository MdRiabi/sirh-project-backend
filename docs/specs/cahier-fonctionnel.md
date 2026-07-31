CAHIER DES CHARGES FONCTIONNEL : SYSTÈME D'INFORMATION RH (SIRH)

**Statut** : transcription fidèle du cahier des charges fonctionnel v1 fourni par le métier, avec deux annotations de traçabilité (Modules 8 et 13) renvoyant vers la clarification actée dans `docs/specs/cahier-technique.md` (§0). Aucun contenu fonctionnel n'a été modifié — seules deux notes encadrées ont été insérées pour éviter toute ambiguïté future sur le rôle Employé.

## 1. Introduction & Objectifs

Ce document définit les spécifications fonctionnelles pour le développement d'une plateforme centralisée de gestion du personnel en environnement On-Premise.

Périmètre d'utilisation : L'application s'ouvre désormais à un portail "Employé" en libre-service strict (consultation planning, saisie de demandes), tout en conservant un "Back-Office" complet réservé à l'encadrement (Managers, Responsables de Département), au Service RH, à la Direction et à l'Administration Technique dédié au super user.

## 2. Architecture Générale & UX/UI (Version Structurée)

### 2.1. Charte Graphique & Ambience Visuelle (UI)

- **Thème Visuel (Dark Mode)** : L'interface adopte un thème sombre natif pour réduire la fatigue visuelle des utilisateurs (RH et Managers) amenés à travailler de longues heures sur des données chiffrées et des tableaux complexes.
- **Lisibilité & Densité** : Utilisation d'un design à forte densité d'information (data-dense) privilégiant des tableaux épurés, des contrastes élevés et des indicateurs de statut lisibles d'un coup d'œil.

### 2.2. Système de Navigation & Structuration d'Écran

L'expérience utilisateur s'articule autour de deux niveaux de navigation complémentaires et d'éléments d'interaction globale :

**A. Barre de Navigation Principale (Top Bar)**

Située en haut de l'écran, elle permet de basculer instantanément entre les grands domaines fonctionnels :
- **Tableau de Bord** : Vue d'ensemble synthétique (Hypervision).
- **Pointages** : Vue hebdomadaire et suivi des présences réelles.
- **Service RH** : Espace d'administration du personnel et de la structure.
- **Machines** : Gestion du parc de pointeuses.
- **Synthèse** : Rapports et consolidations.
- **Éléments Globaux (Zone Droite)** : Cloche de notification (alertes système) ; Profil / Avatar de l'utilisateur connecté.

**B. Barre de Navigation Secondaire (Side Bar)**

S'active latéralement pour accéder aux fonctions spécialisées selon le profil :
- **Espace RH/Admin** : Le Personnel, Départements, Pointage Manuel, Planning, Congés & Autorisations, Statistiques, Calendrier, Gestion des Comptes Utilisateurs.
- **Espace Resp. Département** : Mon Équipe, Gestion du Matériel, Planning (Consultation stricte), Demandes (Autorisations/Congés).
- **Espace Employé** : Mon Planning, Mes Demandes (Autorisations/Congés).
- **Espace Super User** : Le Personnel, Départements, Pointage Manuel, Gestion des Comptes Utilisateurs, Logs Système, Logs des Utilisateurs, Supervision.

> ⚠️ **Note de traçabilité (2026-07-29)** — La sidebar Super User ci-dessus n'inclut pas Planning/Congés & Autorisations/Statistiques/Calendrier, alors que §2.3 attribue au Super User les mêmes droits que le RH. Traité comme un oubli de rédaction : le CDC-T (`cahier-technique.md` §2.1) donne au Super User le superset complet de l'espace RH/Admin, conformément à §2.3.

### 2.3. Modèle d'Accès & Périmètre Utilisateurs

La plateforme n'est plus exclusivement back-office. La matrice des rôles est redéfinie comme suit :

- **Employé ordinaire** : Accès à son propre planning (consultation) et saisie de ses demandes d'autorisations et de congés.
- **Responsable Département** : Consulte le planning de son équipe (aucune gestion/création). Réceptionne les demandes de ses employés, émet un avis (favorable/défavorable) et les transfère au Responsable RH. Peut déposer une demande d'autorisation pour son équipe. Gère le matériel rattaché aux membres de son équipe (ajout, suppression, modification, mise à jour des statuts).
- **Responsable RH (Admin)** : Gère les plannings (création/édition). Reçoit les demandes pré-validées et est le seul à effectuer la validation finale (Approuver/Rejeter). Droit global sur tous les départements, clôture des périodes. Il gère les comptes utilisateurs de l'application, mais n'a aucun accès aux logs.
- **Directeur** : Uniquement la consultation des KPI (tableaux de bord) en lecture seule. Aucune action de validation ou de gestion opérationnelle.
- **Super User (Administrateur Technique)** : Possède les mêmes droits métiers et d'accès que le Responsable RH (y compris la gestion des comptes utilisateurs). Il dispose d'un espace de navigation dédié qui regroupe tous les services de l'Espace RH/Admin, auquel s'ajoute un accès exclusif aux logs système ainsi qu'aux logs de toutes les actions et connexions des autres comptes utilisateurs de l'application.

> ⚠️ **Note de traçabilité (2026-07-29)** — Confirmé avec le métier : la "saisie de ses demandes" de l'Employé ordinaire ne se fait **jamais directement dans le SIRH**. L'employé formule sa demande hors système (voir Module 8, Workflow Nominal, étape 1) ; seul le Manager ou le RH l'enregistre dans l'application. Le seul droit applicatif réel de l'Employé est donc la consultation en lecture seule de son propre planning. Voir `cahier-technique.md` §0 pour le détail de cet arbitrage.

## 3. Spécifications Détaillées par Module

### Module 1 : Gestion des Pointages

**Objectif** : Capter, centraliser et fiabiliser les temps de présence réels.

**Traitements & Règles** :
- Acquisition des données via connexion IP/Port aux pointeuses ou import de fichiers Excel.
- **Règle d'importation stricte** : La taille maximale d'un fichier d'import est fixée à 1 500 lignes. En cas d'erreur de format ou de donnée invalide sur une seule ligne, le système applique un **arrêt immédiat (au 1er échec)**. Le fichier est rejeté en bloc pour protéger l'intégrité de la base de données.
- Détection automatique des oublis et doublons (pointages < 5 minutes).
- Module de correction manuelle exclusif au RH, avec journalisation obligatoire (Qui, Quand, Valeur précédente).

**Workflow Nominal** :
1. **Source A (Automatique)** : La pointeuse IP synchronise les logs bruts vers le système.
2. **Source B (Fichier)** : Le RH télécharge un fichier Excel (≤ 1500 lignes) via l'interface Machines.
3. **Traitement** : Le système parse les lignes, associe les pointages aux matricules employés, détecte le type (Entrée/Sortie), et identifie les anomalies (nombre impair de pointages ou doublons < 5 min).
4. **Restitution** : Mise à jour automatique de la grille hebdomadaire de pointage.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Erreur sur le fichier d'import (Ligne corrompue)* : Déclencheur — la ligne 850 contient une date invalide ou un matricule inconnu. Réaction système — Rollback total immédiat. Aucune ligne n'est insérée. Affichage d'une modale : `[ERREUR IMPORT] Ligne 850 : Matricule 'ATT999' inexistant. L'importation a été annulée.`
- *Incohérence des pointeuses (Doublon réflexe)* : Déclencheur — un employé badge deux fois en 30 secondes. Réaction système — le second pointage est marqué comme `Ignoré (Doublon)` dans le journal d'audit, sans bloquer le premier.

### Module 2 : Gestion de la Planification

**Objectif** : Organiser le temps de travail prévisionnel des équipes (fixe, 2x8, 3x8).

**Traitements & Règles** :
- Génération de plannings et blocage/alerte instantanée en cas de non-respect du repos obligatoire légal (ex : 11h consécutives).
- Génération d'un rapport croisé (Delta) comparant le planning théorique au pointage réel.

**Workflow Nominal** :
1. Le Responsable RH / Super User sélectionne un département et une période (ex : Semaine du 10 au 16).
2. Il applique un modèle de rotation (ex : 3x8) ou assigne des horaires fixes à chaque membre de l'équipe.
3. Le système contrôle en temps réel les contraintes réglementaires.
4. Le Responsable Département consulte le planning de son équipe en lecture seule (simple consultation, aucune modification possible) pour préparer l'organisation de ses équipes.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Violation du Repos Obligatoire (11h)* : Déclencheur — le Responsable RH / Super User tente d'affecter un poste du soir (fin à 23h00) puis un poste du matin (début à 06h00) le lendemain au même employé. Réaction système — Blocage immédiat de l'action avec message rouge : `[VIOLATION LÉGALE] Repos consécutif insuffisant (7h au lieu de 11h minimum). Affectation refusée.`
- *Chevauchement avec une Indisponibilité* : Déclencheur — un planning est affecté à un employé qui a un congé validé sur la même période. Réaction système — alerte visuelle, le système interdit la planification sur ce salarié spécifique et invite à sélectionner un remplaçant.

### Module 3 : Calcul de l'Assiduité

**Objectif** : Traduire les présences en variables de paie.

**Traitements & Règles** :
- Segmentation automatique : Heures normales, heures de nuit, heures majorées.
- Calcul des primes (ex : 100% si mois complet, fractions selon les tranches de retard) et pénalités.
- **Verrouillage** : Une fois le mois clôturé et exporté pour la paie, les données de ce mois deviennent en lecture seule pour éviter toute désynchronisation.

**Workflow Nominal** :
1. En fin de journée/semaine, le moteur croise les pointages réels (Mod. 1) et les plannings (Mod. 2).
2. Le système segmente le temps (Heures normales, majorées, de nuit, retards).
3. En fin de mois, calcul automatique du taux de présence et application des grilles de primes (ex : 100%, 1/2, 1/3, 1/4).
4. Le RH examine le rapport de synthèse, effectue d'éventuels ajustements, puis clique sur **[Clôturer la Paie]**.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Tentative de modification sur un mois clôturé* : Déclencheur — un Manager tente d'ajouter une autorisation ou un pointage sur un mois dont la paie a déjà été validée (M-1). Réaction système — les données sont verrouillées en lecture seule. Un bouton `[Demander Déverrouillage Exceptionnel]` génère une demande formelle transmise à l'Administrateur RH avec obligation de saisir un motif légal.

### Module 4 : Gestion du Personnel & Vue 360° Employé

**Objectif** : Centraliser les données administratives et offrir une vision historique complète d'un salarié.

**Traitements & Règles** :
- Fiche administrative de l'employé (Contrats, coordonnées, qualifications).
- **Vue 360° (Timeline)** : Une section dédiée au sein de la fiche employé permettant d'afficher l'historique complet du salarié, depuis son embauche jusqu'à l'instant T. Filtres dynamiques : le Responsable RH peut filtrer cet historique par catégorie (ex : Pointages, Congés, Autorisations, Évaluations, Attributions matérielles) et par plage de dates (ex : "Voir uniquement les absences de 2022").

**Workflow Nominal** :
1. Le Responsable RH se rend dans "Le Personnel", recherche et ouvre la fiche d'un employé.
2. Il clique sur l'onglet "Historique 360°" (ou Timeline).
3. Le système interroge instantanément la base de données et génère une frise chronologique inversée (du plus récent au plus ancien) listant tous les événements liés à cet employé.
4. Le RH applique un filtre (ex : "Matériel") pour n'afficher que l'historique des outillages rattachés à cet employé.

### Module 5 : Gestion des Outillages

**Objectif** : Suivre la flotte de matériel confiée aux employés.

**Traitements & Règles** :
- La gestion du matériel est exclusivement effectuée par le Responsable Département depuis son propre compte.
- Le Responsable Département peut : rattacher un matériel à un employé de son équipe, modifier ou supprimer ce rattachement.
- Mise à jour du statut du matériel avec les mentions obligatoires : Perdu, Endommagé, Récupéré, Expiré (fin de vie).
- Alertes de maintenance préventive pour le contrôle périodique.

**Workflow Nominal** :
1. Le Responsable Département sélectionne un outil et le rattache au matricule de l'employé.
2. Lors de la restitution, il passe le statut en "Récupéré". S'il est hors d'usage, en "Endommagé" ou "Expiré".

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Perte & endommagement de matériel* : Le Responsable Département passe le statut sur "Perdu". Le système génère une fiche d'anomalie transmise au RH pour traitement financier.

### Module 6 : Calendrier Dynamique

**Objectif** : Gérer le référentiel des jours chômés.

**Traitements & Règles** :
- Importation des jours fériés et configuration des fermetures collectives par le RH.
- Recalcul automatique des soldes de congés en cas de chevauchement avec un jour férié.

**Workflow Nominal** :
1. L'Administrateur RH importe le référentiel des jours fériés ou saisit les dates de fermeture collective.
2. Le système injecte ces jours dans les moteurs de planification et d'assiduité (les jours sont neutralisés et ne comptent pas comme des absences).

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Chevauchement Jours Fériés Mobiles / Congé déjà posé* : Déclencheur — un jour férié mobile (ex : fête religieuse) est validé à une date où un employé avait déjà posé un congé payé. Réaction système — le système recalcule automatiquement le solde de l'employé en recréditant le jour de congé payé, le jour férié étant prioritaire.

### Module 7 : Statistiques Avancées & Archivage

**Objectif** : Aide à la décision et reporting.

**Traitements & Règles** :
- Exports PDF/Excel (Turnover, absentéisme) générés en asynchrone si le calcul dépasse 5 secondes.
- Histogrammes comparatifs, croisés et suivi longitudinal (Mois vs M-1).
- **Règle de Rétention** : les fichiers générés et les historiques lourds sont conservés 12 mois sur le serveur principal, puis transférés automatiquement vers un serveur d'archivage dédié pour libérer l'espace disque.

**Workflow Nominal** :
1. Le RH sélectionne un type de rapport (ex : Turnover annuel) et clique sur `[EXPORTER]`.
2. Si le calcul prend < 5 secondes, le fichier est téléchargé directement.
3. Si le calcul est lourd (> 5 secondes), le système lance une tâche asynchrone et prévient l'utilisateur.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Échec de transfert vers le serveur d'archivage (Cold Storage à 12 mois)* : Déclencheur — la tâche CRON mensuelle tente de purger et de transférer les fichiers de plus de 12 mois, mais le serveur d'archivage dédié est injoignable (panne réseau). Réaction système — le système annule la suppression sur le serveur principal, retente l'action 3 fois, puis émet un rapport d'erreur d'infrastructure transmis au système de logs/admin sans corrompre la base active.

> ⚠️ **Note de traçabilité (2026-07-29)** — `cahier-technique.md` §1.2 clarifie que l'infrastructure actuelle ne prévoit qu'un seul point de montage réseau (avec un sous-dossier `/archives/`), pas un second serveur physiquement isolé. Le comportement de retry/alerte décrit ci-dessus reste valide, mais s'appliquerait alors à une panne du montage réseau global, pas à un incident isolé de l'archivage.

### Module 8 : Gestion des Congés et Indisponibilités

**Objectif** : Gérer les congés et absences prolongées.

**Traitements & Règles** :
- Saisie effectuée par le Manager ou le RH (aucun accès employé).
- Incrémentation mensuelle automatique des soldes de congés.
- Workflow de validation hiérarchique avec possibilité de délégation (N+1 vers N+2).
- Seul le profil RH possède le droit absolu de modifier ou forcer la nature d'une indisponibilité.

**Workflow Nominal** :
1. L'employé formule sa demande de congé **hors système**.
2. Le Manager ou le RH saisit la demande dans l'application.
3. Le RH valide. Le compteur de congés est déduit et le planning est mis à jour.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Solde de congé insuffisant* : Déclencheur — saisie d'une demande de 10 jours alors que l'employé n'a que 4 jours en solde. Réaction système — affichage d'un avertissement orange : `Solde insuffisant (4 jours disponibles)`. Le RH peut choisir de basculer la demande en `Congé sans solde` ou accorder une avance (forçage RH tracé).

> ✅ **Note de traçabilité (2026-07-29)** — Ce module est la référence retenue pour l'accès Employé (aucun accès applicatif) : voir `cahier-technique.md` §0 pour la réconciliation avec la formulation générale du §2.3.

### Module 9 : Évaluation Annuelle

**Objectif** : Suivre l'évolution des compétences.

**Traitements & Règles** :
- Dématérialisation des grilles de notation et des objectifs.
- Génération automatique d'un Plan de Développement Individuel (PDI) si une compétence est évaluée sous le seuil d'exigence.

**Workflow Nominal** :
1. L'entretien physique a lieu. Le Manager saisit les notes et commentaires dans la grille dématérialisée.
2. Le RH effectue la validation finale.
3. Si des compétences clés sont notées sous la moyenne, le système génère automatiquement un Plan de Développement Individuel (PDI).

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Validation incomplète de la grille* : Déclencheur — le Manager valide la grille mais oublie de noter une compétence obligatoire. Réaction système — la soumission est bloquée, les champs non remplis sont surlignés en rouge avec interdiction de passer à l'étape "Entretien Final".

### Module 10 : Tableau de Bord Global

**Objectif** : Hypervision en temps quasi-réel.

**Traitements & Règles** :
- Widgets personnalisables selon le profil (RH ou Manager).
- Remontée prioritaire des alertes critiques (absentéisme inopiné, heures supplémentaires excessives).

**Workflow Nominal** :
1. L'utilisateur (Manager ou RH) se connecte.
2. Le système charge la disposition de ses widgets et interroge le cache pour afficher les KPI en temps quasi-réel.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Surcharge de calcul d'un widget* : Déclencheur — un widget tente de calculer un KPI sur un volume massif de données et prend plus de 5 secondes. Réaction système — le widget affiche un état "Chargement en arrière-plan..." sans bloquer l'affichage du reste du tableau de bord.

### Module 11 : Gestion des Notifications (Triggers Métier)

**Objectif** : Proactivité via alertes push et e-mails automatiques.

**Traitements & Règles** :
- **Alerte Pointage (Hebdomadaire)** : si un employé cumule 3 oublis de badgeage dans la semaine, alerte envoyée au Manager direct.
- **Alerte Sous-activité (Mensuelle)** : si la somme des jours travaillés est ≤ 5 sur les 30 derniers jours pour un contrat actif, alerte de "Risque de carence" envoyée exclusivement au Service RH.

**Workflow Nominal** :
- Règle 1 (Hebdomadaire) : la tâche CRON scanne la table des pointages chaque dimanche soir. Si `COUNT(oublis) = 3`, une alerte est envoyée au Manager.
- Règle 2 (Mensuelle) : la tâche CRON scanne la table le dernier jour du mois. Si `Jours_Travaillés <= 5`, une alerte de "Sous-activité" est envoyée au RH.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Faux Positif sur l'alerte de Sous-activité* : Déclencheur — un employé a travaillé 0 jour car il est en congé maternité ou arrêt maladie longue durée. Réaction système — le script de notification filtre la requête SQL pour exclure les salariés ayant un motif de suspension de contrat légal actif, évitant l'envoi d'alertes inutiles.

### Module 12 : Gestion du Renouvellement des Contrats

**Objectif** : Anticiper les fins de cycles contractuels.

**Traitements & Règles** :
- Génération d'une tâche RH au 5ème mois (préparation échéance 6 mois).
- Génération d'une tâche RH au 11ème mois (préparation échéance 12 mois).
- Statuts de la tâche : `[En cours]`, `[Validé]` (prolonge la base active), `[Non renouvelé]` (déclenche l'offboarding et passe l'employé en inactif).

**Workflow Nominal** :
- À M+5 et M+11, la tâche CRON génère une tâche RH : "Préparer renouvellement".
- Le RH ouvre la tâche et sélectionne un statut : `[Validé]` (prolonge le contrat) ou `[Non renouvelé]`.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Inaction du RH à l'échéance du contrat* : Déclencheur — le contrat arrive à son terme (fin du 6ème ou 12ème mois) et la tâche RH est toujours au statut `[En cours]`. Réaction système — la tâche bascule en statut `[URGENT / EXPIRÉ]` avec une alerte clignotante rouge sur le Dashboard RH. Le contrat n'est pas clôturé automatiquement pour éviter de bloquer le paiement de la paie par erreur.

### Module 13 : Gestion des Autorisations (Absences Courtes)

**Objectif** : Tracer et justifier les absences partielles (retards, sorties médicales/personnelles).

**Traitements & Règles** :
- **Accès restreint** : saisie réalisable uniquement par le Manager de département ou le Service RH.
- **Workflow Nominal** : le Manager saisit la demande (statut En attente). Le RH reçoit une notification et clique sur Valider.
- **Workflow d'Urgence** : le RH saisit directement la demande, qui passe en Validé instantanément.
- **Régularisation contextuelle** : possibilité de créer une autorisation directement depuis une cellule en anomalie (Module 1 - Pointages) pour justifier un départ anticipé ou un retard a posteriori.
- **Traçabilité** : enregistrement strict en base de données de l'identité du Manager ayant initié la demande et du RH l'ayant validée (piste d'audit).

**Workflow Nominal** :
1. **Saisie** : le Manager crée la demande d'autorisation pour un employé (statut `En attente`).
2. **Validation** : le RH consulte sa liste et clique sur `[Valider]`.
3. **Impact** : l'anomalie correspondante dans le tableau des pointages (Mod. 1) est automatiquement effacée/justifiée.

**Cas Exceptionnels & Gestion d'Erreurs** :
- *Saisie Hors Délai (Régularisation tardive)* : Déclencheur — un Manager tente d'entrer une autorisation pour justifier une absence survenue il y a plus de 72 heures. Réaction système — le système bloque la saisie standard : `[DÉLAI DÉPASSÉ] Les régularisations doivent être effectuées sous 72h`. Seul le profil RH peut passer outre ce blocage via un droit d'outrepassation.
- *Dépassement du Quota Mensuel d'Heures* : Déclencheur — une demande porte l'autorisation cumulée de l'employé à 6 heures sur le mois (limite fixée à 4h). Réaction système — la demande est marquée d'un drapeau d'avertissement : `[QUOTA DÉPASSÉ : +2h]`. Le RH peut quand même valider, mais le système lui demande d'indiquer si les 2 heures excédentaires doivent être déduites du salaire ou récupérées.

> ✅ **Note de traçabilité (2026-07-29)** — Comme le Module 8, ce module confirme l'absence d'accès Employé — voir `cahier-technique.md` §0.

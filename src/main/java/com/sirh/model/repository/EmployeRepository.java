package com.sirh.model.repository;

import com.sirh.model.entity.Employe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeRepository
    extends JpaRepository<Employe, UUID>, JpaSpecificationExecutor<Employe> {

  Optional<Employe> findByReferenceAndDeletedAtIsNull(UUID reference);

  // matricule_interne = "badgeId" transmis par les pointeuses Hikvision et par
  // la colonne badgeId des imports Excel (cahier-technique.md §3.2).
  Optional<Employe> findByMatriculeInterneAndDeletedAtIsNull(String matriculeInterne);

  List<Employe> findByDepartement_IdAndDeletedAtIsNull(Long departementId);

  // Incrémentation mensuelle du solde de congés (Module 8, IncrementationSoldeScheduler).
  List<Employe> findByActifTrueAndDeletedAtIsNull();

  // KPI effectif (Lot 11, com.sirh.reporting) : variantes globale/département.
  long countByActifTrueAndDeletedAtIsNull();

  long countByActifTrueAndDepartement_IdAndDeletedAtIsNull(Long departementId);

  long countByDepartement_IdAndDeletedAtIsNull(Long departementId);

  // Évolution des effectifs sur 12 mois glissants (Lot 11, courbe Dashboard) :
  // un employé compte pour un mois donné s'il était déjà embauché à la fin de
  // ce mois et pas encore sorti au début de ce mois. generate_series comble les
  // mois sans variation d'effectif (sinon absents de tout GROUP BY classique).
  @Query(
      value =
          """
          SELECT to_char(m.mois, 'YYYY-MM') AS mois, COUNT(e.id) AS effectif
          FROM generate_series(date_trunc('month', CURRENT_DATE) - interval '11 months',
                                date_trunc('month', CURRENT_DATE), interval '1 month') AS m(mois)
          LEFT JOIN employes e
            ON e.date_embauche <= (m.mois + interval '1 month' - interval '1 day')
           AND (e.date_fin_contrat IS NULL OR e.date_fin_contrat >= m.mois)
           AND (:departementId IS NULL OR e.departement_id = :departementId)
           AND e.deleted_at IS NULL
          GROUP BY m.mois
          ORDER BY m.mois
          """,
      nativeQuery = true)
  List<Object[]> evolutionEffectifsParMois(@Param("departementId") Long departementId);

  // Départs (turnover) par mois sur 12 mois glissants (Lot 11) : un employé
  // compte pour le mois de sa date_fin_contrat, seule trace de sortie en base
  // (pas de colonne dateSortie dédiée).
  @Query(
      value =
          """
          SELECT to_char(m.mois, 'YYYY-MM') AS mois, COUNT(e.id) AS departs
          FROM generate_series(date_trunc('month', CURRENT_DATE) - interval '11 months',
                                date_trunc('month', CURRENT_DATE), interval '1 month') AS m(mois)
          LEFT JOIN employes e
            ON e.date_fin_contrat >= m.mois AND e.date_fin_contrat < (m.mois + interval '1 month')
           AND (:departementId IS NULL OR e.departement_id = :departementId)
           AND e.deleted_at IS NULL
          GROUP BY m.mois
          ORDER BY m.mois
          """,
      nativeQuery = true)
  List<Object[]> departsParMois(@Param("departementId") Long departementId);

  // Rapport "Turnover annuel" (Lot 11, Module 7) : mêmes principes que
  // evolutionEffectifsParMois/departsParMois, mais bornés aux 12 mois civils
  // d'une année donnée plutôt qu'à une fenêtre glissante depuis aujourd'hui.
  @Query(
      value =
          """
          SELECT to_char(m.mois, 'YYYY-MM') AS mois, COUNT(e.id) AS effectif
          FROM generate_series(make_date(:annee, 1, 1), make_date(:annee, 12, 1), interval '1 month') AS m(mois)
          LEFT JOIN employes e
            ON e.date_embauche <= (m.mois + interval '1 month' - interval '1 day')
           AND (e.date_fin_contrat IS NULL OR e.date_fin_contrat >= m.mois)
           AND (:departementId IS NULL OR e.departement_id = :departementId)
           AND e.deleted_at IS NULL
          GROUP BY m.mois
          ORDER BY m.mois
          """,
      nativeQuery = true)
  List<Object[]> evolutionEffectifsAnnee(
      @Param("annee") int annee, @Param("departementId") Long departementId);

  @Query(
      value =
          """
          SELECT to_char(m.mois, 'YYYY-MM') AS mois, COUNT(e.id) AS departs
          FROM generate_series(make_date(:annee, 1, 1), make_date(:annee, 12, 1), interval '1 month') AS m(mois)
          LEFT JOIN employes e
            ON e.date_fin_contrat >= m.mois AND e.date_fin_contrat < (m.mois + interval '1 month')
           AND (:departementId IS NULL OR e.departement_id = :departementId)
           AND e.deleted_at IS NULL
          GROUP BY m.mois
          ORDER BY m.mois
          """,
      nativeQuery = true)
  List<Object[]> departsAnnee(
      @Param("annee") int annee, @Param("departementId") Long departementId);
}

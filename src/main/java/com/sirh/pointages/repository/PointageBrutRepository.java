package com.sirh.pointages.repository;

import com.sirh.pointages.entity.PointageBrut;
import com.sirh.pointages.entity.PointageStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PointageBrutRepository extends JpaRepository<PointageBrut, Long>, JpaSpecificationExecutor<PointageBrut> {

    // Fenêtre de détection des doublons (cahier-technique.md §3.7) : même employé
    // + même pointeuse, tous statuts confondus (on doit voir un IGNORE_DOUBLON
    // existant pour ne pas le re-comparer comme s'il était candidat au statut OK).
    List<PointageBrut> findByEmployeIdAndPointeuseIdAndTimestampPointageBetween(
            UUID employeId, Long pointeuseId, LocalDateTime debut, LocalDateTime fin);

    // Détection d'anomalie "nombre impair de pointages" sur une journée (Module 1).
    List<PointageBrut> findByEmployeIdAndTimestampPointageBetween(
            UUID employeId, LocalDateTime debut, LocalDateTime fin);

    // Justification des anomalies à la validation d'une autorisation (Module 13,
    // com.sirh.demandes.AutorisationService) : appariement employé+jour, la FK
    // pointages_bruts.autorisation_id reste unidirectionnelle.
    List<PointageBrut> findByEmployeIdAndTimestampPointageBetweenAndStatut(
            UUID employeId, LocalDateTime debut, LocalDateTime fin, PointageStatut statut);

    // Contrôle préalable à la clôture de paie (Module 3, com.sirh.assiduite) :
    // taux d'anomalies justifiées sur le mois, tous employés confondus.
    long countByTimestampPointageBetweenAndStatut(LocalDateTime debut, LocalDateTime fin, PointageStatut statut);

    // Alerte hebdomadaire oublis de badgeage (Module 11, com.sirh.notifications) :
    // une seule requête agrégée plutôt qu'une boucle par employé (contrairement
    // à AssiduiteCalculService qui itère par employé pour un calcul bien plus
    // riche par jour) — ici un simple COUNT groupé suffit.
    @Query("SELECT p.employeId, COUNT(p) FROM PointageBrut p " +
            "WHERE p.timestampPointage BETWEEN :debut AND :fin AND p.statut = :statut " +
            "GROUP BY p.employeId")
    List<Object[]> compterAnomaliesParEmploye(LocalDateTime debut, LocalDateTime fin, PointageStatut statut);

    // Anomalies par semaine (Lot 11, graphique Dashboard) : requête native (comme
    // les agrégations mensuelles de EmployeRepository) — groupée aussi par
    // employé pour permettre le filtrage/somme par département en mémoire dans
    // DashboardService, même principe que compterAnomaliesParEmploye ci-dessus.
    @Query(value = """
            SELECT to_char(date_trunc('week', p.timestamp_pointage), 'YYYY-MM-DD') AS semaine,
                   p.employe_id, COUNT(p.id)
            FROM pointages_bruts p
            WHERE p.timestamp_pointage BETWEEN :debut AND :fin AND p.statut = :statut
            GROUP BY date_trunc('week', p.timestamp_pointage), p.employe_id
            """, nativeQuery = true)
    List<Object[]> compterAnomaliesParSemaineEtEmploye(@Param("debut") LocalDateTime debut,
                                                          @Param("fin") LocalDateTime fin,
                                                          @Param("statut") String statut);
}
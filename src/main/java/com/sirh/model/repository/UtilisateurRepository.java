package com.sirh.model.repository;

import com.sirh.model.entity.Utilisateur;
import com.sirh.model.enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

  // "employe" est chargé en LAZY par défaut ; on le force en eager ici car
  // SirhUserDetails/JwtAuthenticationFilter le lisent hors de toute session
  // Hibernate ouverte (pas de @Transactional sur le chemin d'authentification).
  @EntityGraph(attributePaths = "employe")
  Optional<Utilisateur> findByUsernameAndDeletedAtIsNull(String username);

  @EntityGraph(attributePaths = "employe")
  Optional<Utilisateur> findById(Long id);

  @EntityGraph(attributePaths = "employe")
  List<Utilisateur> findAllByDeletedAtIsNullOrderByUsernameAsc();

  boolean existsByEmployeIdAndDeletedAtIsNull(UUID employeId);

  // Résolution du compte applicatif d'un employé donné (ex. manager d'un
  // département, Module 11 alerte hebdomadaire) — au plus un compte par
  // employé (contrainte UNIQUE sur utilisateurs.employe_id).
  Optional<Utilisateur> findByEmployeIdAndDeletedAtIsNull(UUID employeId);

  // Fan-out des notifications RH/SUPERUSER (Module 11 : DEMANDE_A_VALIDER,
  // ALERTE_SOUS_ACTIVITE).
  List<Utilisateur> findByRoleInAndDeletedAtIsNull(List<Role> roles);

  Optional<Utilisateur> findByReferenceAndDeletedAtIsNull(UUID reference);

  Optional<Utilisateur> findByEmployeIdAndRoleAndDeletedAtIsNull(UUID employeId, Role role);
}

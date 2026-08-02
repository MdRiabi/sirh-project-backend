package com.sirh.auth.repository;

import com.sirh.auth.entity.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // "employe" est chargé en LAZY par défaut ; on le force en eager ici car
    // SirhUserDetails/JwtAuthenticationFilter le lisent hors de toute session
    // Hibernate ouverte (pas de @Transactional sur le chemin d'authentification).
    @EntityGraph(attributePaths = "employe")
    Optional<Utilisateur> findByUsername(String username);

    @EntityGraph(attributePaths = "employe")
    Optional<Utilisateur> findById(Long id);

    @EntityGraph(attributePaths = "employe")
    List<Utilisateur> findAllByOrderByUsernameAsc();

    boolean existsByEmployeId(UUID employeId);
}
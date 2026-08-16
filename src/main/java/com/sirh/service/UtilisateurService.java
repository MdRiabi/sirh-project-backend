package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.model.dto.UtilisateurDto;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;
import com.sirh.exception.UtilisateurNotFoundException;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.model.entity.Employe;
import com.sirh.exception.EmployeNotFoundException;
import com.sirh.model.repository.EmployeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                               EmployeRepository employeRepository,
                               PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> findAll() {
        return utilisateurRepository.findAllByDeletedAtIsNullOrderByUsernameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateurByReference(UUID reference) {
        return toDto(getUtilisateurEntityByReference(reference));
    }

    // Non journalisé volontairement : ne fait que dériver le hash, ne persiste rien.
    // Garder le mot de passe en clair hors de tout argument d'une méthode @Audited
    // (AuditAspect sérialise joinPoint.getArgs() tel quel dans audit_logs.nouvelles_valeurs,
    // qui est en lecture SUPERUSER — jamais de secret en clair dans cette table).
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Audited(action = "CREATE_UTILISATEUR", tableCible = "utilisateurs")
    @Transactional
    public UtilisateurDto create(UUID employeId, String username, String passwordHash, Role role) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new EmployeNotFoundException(employeId));

        Utilisateur utilisateur = Utilisateur.builder()
                .employe(employe)
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .compteActif(true)
                .echecsLoginConsecutifs(0)
                .build();
        return toDto(utilisateurRepository.save(utilisateur));
    }

    @Audited(action = "UPDATE_UTILISATEUR", tableCible = "utilisateurs")
    @Transactional
    public UtilisateurDto updateUtilisateurByReference(UUID reference, Role role, boolean compteActif) {
        Utilisateur utilisateur = getUtilisateurEntityByReference(reference);
        utilisateur.setRole(role);
        utilisateur.setCompteActif(compteActif);
        return toDto(utilisateurRepository.save(utilisateur));
    }

    @Audited(action = "RESET_PASSWORD_UTILISATEUR", tableCible = "utilisateurs")
    @Transactional
    public void resetPassword(UUID reference, String passwordHash) {
        Utilisateur utilisateur = getUtilisateurEntityByReference(reference);
        utilisateur.setPasswordHash(passwordHash);
        utilisateur.setEchecsLoginConsecutifs(0);
        utilisateur.setVerrouilleJusquA(null);
        utilisateurRepository.save(utilisateur);
    }

    private Utilisateur getUtilisateurEntityByReference(UUID reference) {
        return utilisateurRepository.findByReferenceAndDeletedAtIsNull(reference).orElseThrow(() -> new
                UtilisateurNotFoundException(reference));
    }

    private UtilisateurDto toDto(Utilisateur utilisateur) {
        var employe = utilisateur.getEmploye();
        return new UtilisateurDto(
                utilisateur.getReference(),
                employe.getId(),
                employe.getNom(),
                employe.getPrenom(),
                utilisateur.getUsername(),
                utilisateur.getRole(),
                utilisateur.getCompteActif(),
                utilisateur.getDernierLogin(),
                utilisateur.getVerrouilleJusquA(),
                utilisateur.getCreatedAt()
        );
    }
}

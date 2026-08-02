package com.sirh.auth.service;

import com.sirh.audit.annotation.Audited;
import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.exception.UtilisateurNotFoundException;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.exception.EmployeNotFoundException;
import com.sirh.personnel.repository.EmployeRepository;
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
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> new UtilisateurNotFoundException(id));
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
    public Utilisateur create(UUID employeId, String username, String passwordHash, Role role) {
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
        return utilisateurRepository.save(utilisateur);
    }

    @Audited(action = "UPDATE_UTILISATEUR", tableCible = "utilisateurs")
    @Transactional
    public Utilisateur update(Long id, Role role, boolean compteActif) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setRole(role);
        utilisateur.setCompteActif(compteActif);
        return utilisateurRepository.save(utilisateur);
    }

    @Audited(action = "RESET_PASSWORD_UTILISATEUR", tableCible = "utilisateurs")
    @Transactional
    public void resetPassword(Long id, String passwordHash) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setPasswordHash(passwordHash);
        utilisateur.setEchecsLoginConsecutifs(0);
        utilisateur.setVerrouilleJusquA(null);
        utilisateurRepository.save(utilisateur);
    }
}
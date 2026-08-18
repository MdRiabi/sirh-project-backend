package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.UtilisateurDto;
import com.sirh.model.entity.Employe;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.enums.Role;
import com.sirh.model.repository.EmployeRepository;
import com.sirh.model.repository.UtilisateurRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

  private final UtilisateurRepository utilisateurRepository;
  private final EmployeRepository employeRepository;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<UtilisateurDto> findAll() {
    return utilisateurRepository.findAllByDeletedAtIsNullOrderByUsernameAsc().stream()
        .map(this::toDto)
        .toList();
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
  public UtilisateurDto createUtilisateur(
      UUID employeId, String username, String passwordHash, Role role) {
    Employe employe =
        employeRepository
            .findById(employeId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.EMPLOYE_NOT_FOUND, new Object[] {employeId}));

    Utilisateur utilisateur =
        Utilisateur.builder()
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
  public UtilisateurDto updateUtilisateurByReference(
      UUID reference, Role role, boolean compteActif) {
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
    return utilisateurRepository
        .findByReferenceAndDeletedAtIsNull(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.UTILISATEUR_NOT_FOUND, new Object[] {reference}));
  }

  private UtilisateurDto toDto(Utilisateur utilisateur) {
    UtilisateurDto dto = modelMapper.map(utilisateur, UtilisateurDto.class);
    // ModelMapper recopie Utilisateur.employe -> UtilisateurDto.employe en clair
    // (mapping imbriqué automatique), sans repasser par le masquage CNSS
    // d'EmployeService.toDto() : on le réapplique explicitement ici.
    dto.getEmploye().setMatriculeCnss(EmployeService.maskCnss(dto.getEmploye().getMatriculeCnss()));
    return dto;
  }
}

package com.sirh.auth.security;

import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class SirhUserDetails implements UserDetails {

    private final Long userId;
    private final UUID employeId;
    private final Long departementId;
    private final String nom;
    private final String prenom;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean compteActif;
    private final boolean verrouille;

    public SirhUserDetails(Utilisateur utilisateur) {
        this.userId = utilisateur.getId();
        this.employeId = utilisateur.getEmploye().getId();
        this.departementId = utilisateur.getEmploye().getDepartementId();
        this.nom = utilisateur.getEmploye().getNom();
        this.prenom = utilisateur.getEmploye().getPrenom();
        this.username = utilisateur.getUsername();
        this.passwordHash = utilisateur.getPasswordHash();
        this.role = utilisateur.getRole();
        this.compteActif = Boolean.TRUE.equals(utilisateur.getCompteActif());
        LocalDateTime verrouilleJusquA = utilisateur.getVerrouilleJusquA();
        this.verrouille = verrouilleJusquA != null && verrouilleJusquA.isAfter(LocalDateTime.now());
    }

    public Long getUserId() {
        return userId;
    }

    public UUID getEmployeId() {
        return employeId;
    }

    public Long getDepartementId() {
        return departementId;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !verrouille;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return compteActif;
    }
}
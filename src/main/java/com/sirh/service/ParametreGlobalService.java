package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.model.dto.ParametreGlobalDto;
import com.sirh.model.entity.ParametreGlobal;
import com.sirh.exception.ParametreNotFoundException;
import com.sirh.model.repository.ParametreGlobalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ParametreGlobalService {

    private final ParametreGlobalRepository parametreGlobalRepository;

    public ParametreGlobalService(ParametreGlobalRepository parametreGlobalRepository) {
        this.parametreGlobalRepository = parametreGlobalRepository;
    }

    @Transactional(readOnly = true)
    public List<ParametreGlobalDto> findAll() {
        return parametreGlobalRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ParametreGlobalDto getParametreGlobalDto(String cle) {
        return toDto(findByCle(cle));
    }

    // Retourne l'entité (pas le DTO) : réutilisé en interne par de nombreux
    // services métier (assiduité, clôture, outillage, alertes...) qui ont
    // besoin d'accéder directement à la valeur brute du paramètre.
    @Transactional(readOnly = true)
    public ParametreGlobal findByCle(String cle) {
        return parametreGlobalRepository.findByCleAndDeletedAtIsNull(cle).orElseThrow(() -> new ParametreNotFoundException(cle));
    }

    // Relu à chaque appel (jamais mis en cache) : un changement de valeur par le
    // RH doit être actif immédiatement, y compris pour un job déjà planifié —
    // cf. CLAUDE.md, Redis borné au dashboard/JWT, jamais réutilisé ici.
    @Transactional(readOnly = true)
    public BigDecimal valeurDecimale(String cle) {
        return new BigDecimal(findByCle(cle).getValeur());
    }

    @Audited(action = "UPDATE_PARAMETRE_GLOBAL", tableCible = "parametres_globaux")
    @Transactional
    public ParametreGlobalDto modifierValeur(String cle, String valeur, UUID modifieParId) {
        ParametreGlobal parametre = findByCle(cle);
        parametre.setValeur(valeur);
        parametre.setModifieParId(modifieParId);
        return toDto(parametreGlobalRepository.save(parametre));
    }

    private ParametreGlobalDto toDto(ParametreGlobal p) {
        return new ParametreGlobalDto(p.getReference(), p.getCle(), p.getValeur(), p.getType(), p.getDescription(), p.getUpdatedAt());
    }
}

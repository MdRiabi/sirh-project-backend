package com.sirh.parametres.service;

import com.sirh.audit.annotation.Audited;
import com.sirh.parametres.entity.ParametreGlobal;
import com.sirh.parametres.exception.ParametreNotFoundException;
import com.sirh.parametres.repository.ParametreGlobalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ParametreGlobalService {

    private final ParametreGlobalRepository parametreGlobalRepository;

    public ParametreGlobalService(ParametreGlobalRepository parametreGlobalRepository) {
        this.parametreGlobalRepository = parametreGlobalRepository;
    }

    @Transactional(readOnly = true)
    public List<ParametreGlobal> findAll() {
        return parametreGlobalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ParametreGlobal findByCle(String cle) {
        return parametreGlobalRepository.findByCle(cle).orElseThrow(() -> new ParametreNotFoundException(cle));
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
    public ParametreGlobal modifierValeur(String cle, String valeur, UUID modifieParId) {
        ParametreGlobal parametre = findByCle(cle);
        parametre.setValeur(valeur);
        parametre.setModifieParId(modifieParId);
        parametre.setUpdatedAt(LocalDateTime.now());
        return parametreGlobalRepository.save(parametre);
    }
}

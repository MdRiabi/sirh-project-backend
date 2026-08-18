package com.sirh.model.dto;

import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// POST /fichiers/demande-upload — entiteType/entiteId optionnels (rattachement
// à une entité métier facultatif, ex. AVATAR n'est rattaché à rien).
public record FichierDemandeUploadRequest(
    @NotBlank String nomOriginal,
    @NotBlank String typeMime,
    @NotNull @Positive Long tailleOctets,
    @NotNull FichierCategorie categorie,
    EntiteType entiteType,
    Long entiteId) {}

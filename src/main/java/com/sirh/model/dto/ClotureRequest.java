package com.sirh.model.dto;

import jakarta.validation.constraints.NotBlank;

// mois en String (pas YearMonth) : Springdoc reflète YearMonth comme un objet
// structuré {year, month, monthValue, leapYear} dans le schéma OpenAPI (donc
// dans les types TS générés côté frontend), alors que Jackson accepte en
// réalité une chaîne ISO "YYYY-MM" au runtime — écart schéma/réalité qui
// casserait le contrat consommé par openapi-typescript. Parsé via
// YearMonth.parse() dans le contrôleur, même convention que les query params
// GET (calcul-mensuel, cloture-paie, demandes-deverrouillage).
public record ClotureRequest(@NotBlank String mois) {}

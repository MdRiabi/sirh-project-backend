package com.sirh.exception;

import java.util.List;

/**
 * Grille de notation soumise avec au moins une compétence non notée (Module 9,
 * cas exceptionnel : "Validation incomplète de la grille"). Portée par
 * EvaluationService#mettreAJour — la mise à jour est tout de même persistée
 * avant le rejet, pour que le Manager ne perde pas sa saisie.
 */
public class EvaluationIncompleteException extends RuntimeException {

    private final List<String> competencesManquantes;

    public EvaluationIncompleteException(List<String> competencesManquantes) {
        super("Compétence(s) obligatoire(s) non notée(s) : " + String.join(", ", competencesManquantes));
        this.competencesManquantes = competencesManquantes;
    }

    public List<String> getCompetencesManquantes() {
        return competencesManquantes;
    }
}

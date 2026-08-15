package com.sirh.exception;

/**
 * Erreur de validation sur une ligne du fichier d'import Excel — porte le
 * numéro de ligne pour l'arrêt immédiat au 1er échec (cahier-fonctionnel.md
 * Module 1 : "Règle d'importation stricte").
 */
public class ImportPointagesException extends RuntimeException {

    private final int ligne;

    public ImportPointagesException(int ligne, String message) {
        super(message);
        this.ligne = ligne;
    }

    public int getLigne() {
        return ligne;
    }
}
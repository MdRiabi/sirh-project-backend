package com.sirh.reporting.dto;

import org.springframework.http.MediaType;

// Contenu binaire prêt à télécharger (PDF ou Excel), en mémoire — volumétrie
// modeste (cf. ARCHITECTURE.md §7), pas besoin de streaming fichier ici.
public record RapportGenere(String nomFichier, byte[] contenu, MediaType contentType) {
}

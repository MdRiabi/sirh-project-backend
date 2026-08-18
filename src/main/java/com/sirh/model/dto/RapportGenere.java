package com.sirh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;

// Contenu binaire prêt à télécharger (PDF ou Excel), en mémoire — volumétrie
// modeste (cf. ARCHITECTURE.md §7), pas besoin de streaming fichier ici.
// Retourné directement en corps de réponse HTTP binaire (pas de sérialisation
// JSON), d'où l'absence de @JsonProperty ici.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RapportGenere {
  private String nomFichier;
  private byte[] contenu;
  private MediaType contentType;
}

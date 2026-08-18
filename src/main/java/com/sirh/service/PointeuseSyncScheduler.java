package com.sirh.service;

import com.sirh.model.entity.Pointeuse;
import com.sirh.model.repository.PointeuseRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fallback horaire ISAPI (cahier-technique.md §1.1 : "fallback horaire @Scheduled sur
 * /ISAPI/Attendance/Search") — si le mode Push échoue, le backend interroge directement chaque
 * pointeuse.
 *
 * <p>ponytail: squelette réel (requête HTTP + mise à jour de statut_en_ligne), mais le protocole
 * ISAPI Hikvision réel (authentification digest, réponse XML propriétaire "AccessControllerEvent")
 * n'a pas pu être vérifié sans matériel physique — fabriquer un parseur pour un schéma non confirmé
 * risquerait de donner une fausse confiance. Le parsing des événements d'assiduité renvoyés par cet
 * appel reste donc à finaliser lors de l'intégration avec de vraies pointeuses ; pour l'instant ce
 * job ne fait que sonder la connectivité (statut_en_ligne / derniere_synchro). Le canal principal
 * et pleinement fonctionnel reste le webhook Push (HikvisionController).
 */
@Component
public class PointeuseSyncScheduler {

  private static final Logger log = LoggerFactory.getLogger(PointeuseSyncScheduler.class);

  private final PointeuseRepository pointeuseRepository;
  private final RestClient restClient;

  public PointeuseSyncScheduler(PointeuseRepository pointeuseRepository) {
    this.pointeuseRepository = pointeuseRepository;
    this.restClient = RestClient.builder().requestFactory(clientHttpRequestFactory()).build();
  }

  private static org.springframework.http.client.ClientHttpRequestFactory
      clientHttpRequestFactory() {
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(5000);
    return factory;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void synchroniserToutesLesPointeuses() {
    List<Pointeuse> pointeuses = pointeuseRepository.findAll();
    log.info("Fallback ISAPI horaire : sondage de {} pointeuse(s)", pointeuses.size());
    for (Pointeuse pointeuse : pointeuses) {
      synchroniser(pointeuse);
    }
  }

  private void synchroniser(Pointeuse pointeuse) {
    String url =
        "http://"
            + pointeuse.getAdresseIp()
            + ":"
            + pointeuse.getPort()
            + "/ISAPI/Attendance/Search";
    try {
      restClient.get().uri(url).retrieve().toBodilessEntity();
      pointeuse.setStatutEnLigne(true);
      pointeuse.setDerniereSynchro(LocalDateTime.now());
    } catch (Exception e) {
      log.debug(
          "Pointeuse {} ({}) injoignable en fallback ISAPI : {}",
          pointeuse.getId(),
          pointeuse.getAdresseIp(),
          e.getMessage());
      pointeuse.setStatutEnLigne(false);
    }
    pointeuseRepository.save(pointeuse);
  }
}

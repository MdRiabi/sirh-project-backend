package com.sirh.notifications.listener;

import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.notifications.entity.NotificationType;
import com.sirh.notifications.event.DemandeCreeEvent;
import com.sirh.notifications.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Découplage Lot 5 -> Lot 8 (cf. plan Lot 8) : AutorisationService/
 * IndisponibiliteService ignorent tout de notifications, ils publient un
 * DemandeCreeEvent via ApplicationEventPublisher. AFTER_COMMIT : une demande
 * qui rollback (ex. mois clôturé, cf. cloturePaieService.verifierMoisOuvert)
 * ne doit jamais générer de notification fantôme. @Async : ne bloque jamais
 * la requête HTTP du Manager qui saisit la demande.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationEventListener(NotificationService notificationService, UtilisateurRepository utilisateurRepository) {
        this.notificationService = notificationService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDemandeCreee(DemandeCreeEvent event) {
        String message = "Nouvelle demande à valider (%s #%d, %s)".formatted(
                event.typeDemande(), event.demandeId(), event.libelle());
        List<Utilisateur> destinataires = utilisateurRepository.findByRoleIn(List.of(Role.ROLE_RH, Role.ROLE_SUPERUSER));
        for (Utilisateur destinataire : destinataires) {
            notificationService.creer(destinataire.getId(), NotificationType.DEMANDE_A_VALIDER, message,
                    event.typeDemande(), event.demandeId());
        }
    }
}

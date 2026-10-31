package sn.cse.notification.application.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import sn.cse.notification.application.port.out.AnnuaireRepository;
import sn.cse.notification.application.port.out.MailPort;
import sn.cse.notification.application.port.out.MessageEmail;
import sn.cse.notification.application.port.out.NotificationDiffuseur;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/**
 * Diffusion (SSE + e-mail) des notifications après validation de la transaction. Un échec de
 * diffusion est journalisé et n'interrompt ni les autres envois ni le consommateur Kafka : la
 * notification reste consultable dans l'application (pas de tempête de rejeux).
 */
@Service
public class DiffusionNotificationsService {

  private static final Logger LOG = LoggerFactory.getLogger(DiffusionNotificationsService.class);

  private final NotificationDiffuseur diffuseur;
  private final MailPort mail;
  private final AnnuaireRepository annuaire;

  /**
   * @param diffuseur diffusion temps réel
   * @param mail envoi d'e-mails
   * @param annuaire annuaire (adresses)
   */
  public DiffusionNotificationsService(
      NotificationDiffuseur diffuseur, MailPort mail, AnnuaireRepository annuaire) {
    this.diffuseur = diffuseur;
    this.mail = mail;
    this.annuaire = annuaire;
  }

  /**
   * @param evenement notifications créées dans la transaction validée
   */
  @TransactionalEventListener(fallbackExecution = true)
  public void apresCreation(NotificationsCreees evenement) {
    for (NotificationsCreees.Creee creee : evenement.notifications()) {
      Notification n = creee.notification();
      try {
        diffuseur.diffuser(n);
      } catch (RuntimeException e) {
        LOG.warn("Diffusion temps réel impossible pour la notification {}", n.id(), e);
      }
      if (creee.email()) {
        envoyerEmails(n);
      }
    }
  }

  private void envoyerEmails(Notification n) {
    try {
      for (UtilisateurAnnuaire u : destinataires(n.destinataire())) {
        if (u.joignableParEmail()) {
          mail.envoyer(
              new MessageEmail(
                  u.email(), u.nomComplet(), n.titre(), n.message(), n.niveau(), n.lien()));
        }
      }
    } catch (RuntimeException e) {
      LOG.warn("Envoi des e-mails impossible pour la notification {}", n.id(), e);
    }
  }

  private List<UtilisateurAnnuaire> destinataires(Destinataire destinataire) {
    if (destinataire instanceof Destinataire.Utilisateur u) {
      return annuaire.parId(u.id()).stream().toList();
    }
    return annuaire.actifsParRole(((Destinataire.Role) destinataire).code());
  }
}

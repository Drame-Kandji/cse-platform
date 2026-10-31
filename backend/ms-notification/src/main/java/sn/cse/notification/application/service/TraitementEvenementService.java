package sn.cse.notification.application.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.notification.application.port.in.TraiterEvenementUseCase;
import sn.cse.notification.application.port.out.AnnuaireRepository;
import sn.cse.notification.application.port.out.NotificationRepository;
import sn.cse.notification.application.port.out.ReglesRoutageSource;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationPlanifiee;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;
import sn.cse.notification.domain.service.FormateurValeurs;
import sn.cse.notification.domain.service.MoteurRoutage;

/** Routage d'un événement métier vers des notifications persistées. */
@Service
public class TraitementEvenementService implements TraiterEvenementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(TraitementEvenementService.class);

  private final MoteurRoutage moteur;
  private final NotificationRepository notifications;
  private final AnnuaireRepository annuaire;
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  /**
   * @param regles règles configurées
   * @param notifications persistance
   * @param annuaire annuaire local
   * @param publisher publication de l'événement applicatif post-commit
   * @param clock horloge
   */
  public TraitementEvenementService(
      ReglesRoutageSource regles,
      NotificationRepository notifications,
      AnnuaireRepository annuaire,
      ApplicationEventPublisher publisher,
      Clock clock) {
    this.moteur =
        new MoteurRoutage(
            regles.regles(), new FormateurValeurs(regles.libelles()), regles.valeurAbsente());
    this.notifications = notifications;
    this.annuaire = annuaire;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Override
  @Transactional
  public List<Notification> traiter(EvenementMetier evenement) {
    List<NotificationPlanifiee> plans = moteur.router(evenement);
    if (plans.isEmpty()) {
      return List.of();
    }
    List<NotificationsCreees.Creee> creees = new ArrayList<>();
    for (NotificationPlanifiee plan : plans) {
      Set<UUID> chefs =
          plan.chantierPourChefs() == null
              ? Set.of()
              : annuaire.chefsDuChantier(plan.chantierPourChefs()).stream()
                  .map(UtilisateurAnnuaire::id)
                  .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
      plan.materialiser(chefs, evenement.eventId(), clock.instant())
          .forEach(n -> creees.add(new NotificationsCreees.Creee(n, plan.email())));
    }
    List<Notification> resultat =
        creees.stream().map(NotificationsCreees.Creee::notification).toList();
    notifications.enregistrer(resultat);
    publisher.publishEvent(new NotificationsCreees(creees));
    LOG.info(
        "Événement {} ({}) : {} notification(s) créée(s)",
        evenement.type(),
        evenement.eventId(),
        resultat.size());
    return resultat;
  }
}

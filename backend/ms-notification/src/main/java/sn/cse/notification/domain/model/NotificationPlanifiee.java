package sn.cse.notification.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Résultat de l'application d'une règle à un événement : contenu résolu et destinataires connus.
 * Les chefs du chantier, s'ils sont demandés, sont ajoutés lors de la matérialisation (ils
 * proviennent de l'annuaire, hors du domaine).
 *
 * @param type type d'événement
 * @param titre titre résolu
 * @param message message résolu
 * @param niveau criticité
 * @param lien lien résolu ou {@code null}
 * @param entite type d'entité ou {@code null}
 * @param entiteId identifiant d'entité ou {@code null}
 * @param roles rôles destinataires
 * @param utilisateurs utilisateurs destinataires lus dans le payload
 * @param chantierPourChefs chantier dont les chefs doivent être notifiés, ou {@code null}
 * @param email vrai pour envoyer aussi un e-mail
 */
public record NotificationPlanifiee(
    String type,
    String titre,
    String message,
    NiveauNotification niveau,
    String lien,
    String entite,
    String entiteId,
    Set<String> roles,
    Set<UUID> utilisateurs,
    UUID chantierPourChefs,
    boolean email) {

  public NotificationPlanifiee {
    Objects.requireNonNull(type, "type");
    roles = Set.copyOf(roles);
    utilisateurs = Set.copyOf(utilisateurs);
  }

  /**
   * Crée les notifications : une par rôle, une par utilisateur (sans doublon).
   *
   * @param chefs chefs du chantier trouvés dans l'annuaire
   * @param eventId événement source
   * @param maintenant date de création
   * @return les notifications à enregistrer
   */
  public List<Notification> materialiser(Collection<UUID> chefs, UUID eventId, Instant maintenant) {
    List<Notification> resultat = new ArrayList<>();
    roles.stream()
        .sorted()
        .forEach(r -> resultat.add(creer(Destinataire.role(r), eventId, maintenant)));
    Set<UUID> individus = new LinkedHashSet<>(utilisateurs);
    individus.addAll(chefs);
    individus.forEach(u -> resultat.add(creer(Destinataire.utilisateur(u), eventId, maintenant)));
    return resultat;
  }

  private Notification creer(Destinataire destinataire, UUID eventId, Instant maintenant) {
    return new Notification(
        UUID.randomUUID(),
        type,
        titre,
        message,
        niveau,
        lien,
        entite,
        entiteId,
        destinataire,
        eventId,
        maintenant);
  }
}

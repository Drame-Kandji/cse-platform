package sn.cse.notification.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Règle de routage (configurée en YAML) : quel événement notifie qui, avec quel niveau, quels
 * textes et quel lien. Plusieurs règles peuvent viser le même type d'événement (ex. une règle par
 * destinataire ou par gravité).
 *
 * @param evenement type d'événement visé
 * @param roles rôles destinataires
 * @param champsDestinataires champs du payload contenant l'identifiant d'un utilisateur
 * @param chefsDuChantier vrai pour notifier les chefs du chantier du payload (via l'annuaire)
 * @param champChantier champ du payload contenant l'identifiant du chantier
 * @param niveau criticité
 * @param titre modèle du titre (marqueurs {@code {champ}})
 * @param message modèle du message
 * @param lien modèle du lien frontend (ignoré si un marqueur est introuvable), ou {@code null}
 * @param entite type d'entité exposé au frontend, ou {@code null}
 * @param champEntiteId champ du payload donnant l'identifiant de l'entité, ou {@code null}
 * @param email vrai pour envoyer aussi un e-mail
 * @param condition condition supplémentaire, ou {@code null}
 */
public record RegleRoutage(
    String evenement,
    Set<String> roles,
    List<String> champsDestinataires,
    boolean chefsDuChantier,
    String champChantier,
    NiveauNotification niveau,
    String titre,
    String message,
    String lien,
    String entite,
    String champEntiteId,
    boolean email,
    ConditionRegle condition) {

  public RegleRoutage {
    Objects.requireNonNull(evenement, "evenement");
    Objects.requireNonNull(niveau, "niveau");
    Objects.requireNonNull(titre, "titre");
    Objects.requireNonNull(message, "message");
    roles = roles == null ? Set.of() : Set.copyOf(roles);
    champsDestinataires =
        champsDestinataires == null ? List.of() : List.copyOf(champsDestinataires);
    if (roles.isEmpty() && champsDestinataires.isEmpty() && !chefsDuChantier) {
      throw new IllegalArgumentException("La règle " + evenement + " n'a aucun destinataire");
    }
    if (chefsDuChantier && (champChantier == null || champChantier.isBlank())) {
      throw new IllegalArgumentException("La règle " + evenement + " exige un champ chantier");
    }
  }

  /**
   * @param evenementRecu événement reçu
   * @return vrai si la règle s'applique à cet événement
   */
  public boolean sApplique(EvenementMetier evenementRecu) {
    return evenement.equals(evenementRecu.type())
        && (condition == null || condition.estSatisfaite(evenementRecu));
  }
}

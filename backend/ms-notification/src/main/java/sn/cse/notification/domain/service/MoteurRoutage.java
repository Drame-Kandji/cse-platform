package sn.cse.notification.domain.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.NotificationPlanifiee;
import sn.cse.notification.domain.model.RegleRoutage;

/**
 * Applique les règles de routage (Chain of Responsibility déclarative) à un événement : chaque
 * règle applicable produit une {@link NotificationPlanifiee} au contenu résolu.
 */
public class MoteurRoutage {

  private final List<RegleRoutage> regles;
  private final FormateurValeurs formateur;
  private final String valeurAbsente;

  /**
   * @param regles règles configurées
   * @param formateur mise en forme des valeurs
   * @param valeurAbsente texte de repli pour un marqueur introuvable dans un titre ou un message
   */
  public MoteurRoutage(
      List<RegleRoutage> regles, FormateurValeurs formateur, String valeurAbsente) {
    this.regles = List.copyOf(regles);
    this.formateur = formateur;
    this.valeurAbsente = valeurAbsente;
  }

  /**
   * @param evenement événement reçu
   * @return les notifications planifiées (vide si aucune règle ne s'applique)
   */
  public List<NotificationPlanifiee> router(EvenementMetier evenement) {
    return regles.stream()
        .filter(r -> r.sApplique(evenement))
        .map(r -> planifier(r, evenement))
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * @param type type d'événement
   * @return vrai si au moins une règle vise ce type
   */
  public boolean estRoute(String type) {
    return regles.stream().anyMatch(r -> r.evenement().equals(type));
  }

  private Optional<NotificationPlanifiee> planifier(RegleRoutage regle, EvenementMetier evt) {
    Set<UUID> utilisateurs = new LinkedHashSet<>();
    regle.champsDestinataires().forEach(champ -> evt.uuid(champ).ifPresent(utilisateurs::add));
    UUID chantier = regle.chefsDuChantier() ? evt.uuid(regle.champChantier()).orElse(null) : null;
    if (regle.roles().isEmpty() && utilisateurs.isEmpty() && chantier == null) {
      // Aucun destinataire résolu (ex. demandeur absent du payload) : rien à notifier.
      return Optional.empty();
    }
    ModeleTexte.Resolution lien =
        ModeleTexte.resoudre(regle.lien(), champ -> evt.valeur(champ).map(String::valueOf), "");
    String entiteId =
        regle.champEntiteId() == null
            ? null
            : evt.valeur(regle.champEntiteId()).map(String::valueOf).orElse(null);
    return Optional.of(
        new NotificationPlanifiee(
            evt.type(),
            texte(regle.titre(), evt),
            texte(regle.message(), evt),
            regle.niveau(),
            lien != null && lien.complet() ? lien.texte() : null,
            regle.entite(),
            entiteId,
            regle.roles(),
            utilisateurs,
            chantier,
            regle.email()));
  }

  private String texte(String modele, EvenementMetier evt) {
    return ModeleTexte.resoudre(
            modele, champ -> evt.valeur(champ).map(formateur::formater), valeurAbsente)
        .texte();
  }
}

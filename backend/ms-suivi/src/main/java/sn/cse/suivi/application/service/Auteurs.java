package sn.cse.suivi.application.service;

import java.util.Set;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.domain.model.Mouvement;

/** Auteurs des opérations (utilisateur authentifié ou système). */
public final class Auteurs {

  /** Utilisateur technique des traitements automatiques (jobs, événements Kafka, amorçage). */
  public static final CurrentUser SYSTEME =
      new CurrentUser(null, "Système", null, Set.of(), Set.of(), null);

  private Auteurs() {}

  /**
   * @param utilisateur utilisateur courant
   * @return l'auteur d'un mouvement
   */
  public static Mouvement.Auteur de(CurrentUser utilisateur) {
    return new Mouvement.Auteur(utilisateur.id(), utilisateur.nom());
  }
}

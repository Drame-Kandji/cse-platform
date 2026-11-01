package sn.cse.auth.application.service;

import java.util.Objects;
import sn.cse.auth.application.port.out.LienActivation;

/**
 * Paramètres des comptes du portail fournisseur (issus de {@code cse.auth.portail-fournisseur.*}).
 *
 * @param prenomContact prénom affiché du compte (le nom est la raison sociale), faute de nom
 *     d'interlocuteur dans {@code FournisseurPayload}
 * @param lien lien d'activation envoyé par e-mail
 */
public record ParametresPortailFournisseur(String prenomContact, LienActivation lien) {

  public ParametresPortailFournisseur {
    if (prenomContact == null || prenomContact.isBlank()) {
      throw new IllegalArgumentException("Prénom de contact obligatoire");
    }
    Objects.requireNonNull(lien, "lien");
  }
}

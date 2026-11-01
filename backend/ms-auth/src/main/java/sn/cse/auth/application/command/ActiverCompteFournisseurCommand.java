package sn.cse.auth.application.command;

import java.util.UUID;

/**
 * Validation d'une inscription fournisseur reçue de ms-fournisseurs ({@code FOURNISSEUR_VALIDE}).
 *
 * @param fournisseurId fiche fournisseur validée
 * @param email e-mail du fournisseur, identifiant du compte du portail
 * @param raisonSociale raison sociale (nom affiché du compte faute d'interlocuteur)
 * @param contactNom prénom et nom de l'interlocuteur déclaré à l'inscription, peut être absent
 */
public record ActiverCompteFournisseurCommand(
    UUID fournisseurId, String email, String raisonSociale, String contactNom) {

  /** Commande sans interlocuteur connu. */
  public ActiverCompteFournisseurCommand(UUID fournisseurId, String email, String raisonSociale) {
    this(fournisseurId, email, raisonSociale, null);
  }

  public ActiverCompteFournisseurCommand {
    if (fournisseurId == null) {
      throw new IllegalArgumentException("Identifiant du fournisseur absent de l'événement");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("E-mail du fournisseur absent de l'événement");
    }
    email = email.strip().toLowerCase(java.util.Locale.ROOT);
    contactNom = contactNom == null || contactNom.isBlank() ? null : contactNom.strip();
  }
}

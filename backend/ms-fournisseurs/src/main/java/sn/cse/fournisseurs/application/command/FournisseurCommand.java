package sn.cse.fournisseurs.application.command;

import java.util.List;
import java.util.Set;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.CoordonneesBancaires;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;

/**
 * Données de référentiel saisies pour créer ou modifier un fournisseur.
 *
 * @param raisonSociale raison sociale
 * @param ninea NINEA
 * @param rccm RCCM
 * @param adresse adresse
 * @param ville ville
 * @param pays pays
 * @param telephone téléphone
 * @param email e-mail
 * @param contactNom interlocuteur
 * @param contactFonction fonction de l'interlocuteur
 * @param specialites spécialités
 * @param certifications certifications
 * @param banque banque (optionnelle)
 * @param iban IBAN (optionnel, avec la banque)
 */
public record FournisseurCommand(
    String raisonSociale,
    String ninea,
    String rccm,
    String adresse,
    String ville,
    String pays,
    String telephone,
    String email,
    String contactNom,
    String contactFonction,
    Set<CategorieMateriel> specialites,
    List<String> certifications,
    String banque,
    String iban) {

  /**
   * @return profil de domaine validé
   */
  public ProfilFournisseur profil() {
    return new ProfilFournisseur(
        raisonSociale,
        ninea,
        rccm,
        adresse,
        ville,
        pays,
        telephone,
        email,
        contactNom,
        contactFonction,
        specialites,
        certifications);
  }

  /**
   * @return coordonnées bancaires, ou {@code null} si aucune n'est fournie
   */
  public CoordonneesBancaires coordonnees() {
    boolean vide = (banque == null || banque.isBlank()) && (iban == null || iban.isBlank());
    return vide ? null : new CoordonneesBancaires(banque, iban);
  }
}

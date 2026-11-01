package sn.cse.fournisseurs.application.command;

import java.util.List;
import java.util.Set;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;

/**
 * Auto-inscription publique d'un fournisseur sur le portail (contrat §13, {@code Inscription}). Ni
 * certifications ni coordonnées bancaires : elles sont renseignées après validation.
 *
 * @param raisonSociale raison sociale
 * @param ninea NINEA
 * @param rccm RCCM (optionnel)
 * @param adresse adresse (optionnelle)
 * @param ville ville (optionnelle)
 * @param pays pays
 * @param telephone téléphone (optionnel)
 * @param email e-mail de contact, futur identifiant de connexion au portail
 * @param contactNom interlocuteur
 * @param contactFonction fonction de l'interlocuteur (optionnelle)
 * @param specialites spécialités (au moins une)
 */
public record InscriptionCommand(
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
    Set<CategorieMateriel> specialites) {

  /**
   * @return profil de domaine validé et normalisé
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
        List.of());
  }
}

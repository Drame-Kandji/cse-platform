package sn.cse.fournisseurs.domain.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Informations légales et de contact d'un fournisseur (référentiel).
 *
 * @param raisonSociale raison sociale
 * @param ninea numéro NINEA (unique)
 * @param rccm numéro RCCM (optionnel)
 * @param adresse adresse postale
 * @param ville ville
 * @param pays pays
 * @param telephone téléphone (+221…)
 * @param email e-mail de contact
 * @param contactNom nom de l'interlocuteur
 * @param contactFonction fonction de l'interlocuteur
 * @param specialites catégories de matériel fournies (au moins une)
 * @param certifications certifications déclarées (ISO 9001…)
 */
public record ProfilFournisseur(
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
    List<String> certifications) {

  /** Valide et normalise le profil. */
  public ProfilFournisseur {
    raisonSociale = obligatoire(raisonSociale, "raison sociale");
    ninea = obligatoire(ninea, "NINEA").toUpperCase(java.util.Locale.ROOT);
    pays = obligatoire(pays, "pays");
    email = obligatoire(email, "e-mail").toLowerCase(java.util.Locale.ROOT);
    if (specialites == null || specialites.isEmpty()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Au moins une spécialité doit être renseignée.");
    }
    specialites = Set.copyOf(EnumSet.copyOf(specialites));
    certifications =
        certifications == null
            ? List.of()
            : certifications.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
  }

  /**
   * @param categorie catégorie de matériel
   * @return {@code true} si le fournisseur déclare cette spécialité
   */
  public boolean couvre(CategorieMateriel categorie) {
    return specialites.contains(categorie);
  }

  private static String obligatoire(String valeur, String libelle) {
    if (valeur == null || valeur.isBlank()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le champ « " + libelle + " » est obligatoire.");
    }
    return valeur.trim();
  }
}

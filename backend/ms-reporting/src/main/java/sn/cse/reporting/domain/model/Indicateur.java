package sn.cse.reporting.domain.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import sn.cse.common.exception.InvalidRequestException;

/**
 * Indicateurs disponibles en série temporelle ({@code GET /api/reporting/series}). Un indicateur
 * est accessible si l'un des profils autorisés de l'utilisateur affiche l'une de ses sections.
 */
public enum Indicateur {
  /**
   * Disponibilité opérationnelle = (total − en maintenance − réformés) / (total − réformés), en %,
   * d'après les instantanés quotidiens du parc (reconstitués avant le premier enregistré).
   */
  TAUX_DISPONIBILITE("Disponibilité opérationnelle (%)", true, EnumSet.of(Section.PARC)),
  /** Jours affectés / jours disponibles, en %. */
  TAUX_UTILISATION("Taux d'utilisation (%)", true, EnumSet.of(Section.PARC)),
  /** Montant des engagements validés (FCFA). */
  MONTANT_ENGAGE("Montant engagé (FCFA)", false, EnumSet.of(Section.FINANCES, Section.CHANTIERS)),
  /** Coût des maintenances clôturées (FCFA). */
  COUT_MAINTENANCE(
      "Coût de maintenance (FCFA)", false, EnumSet.of(Section.FINANCES, Section.CHANTIERS)),
  /** Nombre de besoins soumis. */
  BESOINS_SOUMIS("Besoins soumis", false, EnumSet.of(Section.BESOINS)),
  /** Délai moyen publication de l'AO → livraison réelle, en jours. */
  DELAI_APPROVISIONNEMENT(
      "Délai moyen d'approvisionnement (jours)", true, EnumSet.of(Section.ACHATS));

  private final String libelle;
  private final boolean ratio;
  private final Set<Section> sections;

  Indicateur(String libelle, boolean ratio, Set<Section> sections) {
    this.libelle = libelle;
    this.ratio = ratio;
    this.sections = Set.copyOf(sections);
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }

  /**
   * @return vrai pour un taux ou une moyenne : une tranche sans donnée ne produit pas de point (un
   *     zéro serait trompeur) ; faux pour un cumul, où une tranche vide vaut 0
   */
  public boolean estRatio() {
    return ratio;
  }

  /**
   * @param profils profils autorisés de l'utilisateur
   * @return vrai si l'un des profils affiche une section de l'indicateur
   */
  public boolean accessiblePour(List<ProfilTableau> profils) {
    return profils.stream().anyMatch(p -> sections.stream().anyMatch(p::affiche));
  }

  /**
   * @param code code demandé
   * @return l'indicateur
   * @throws InvalidRequestException si le code est inconnu
   */
  public static Indicateur depuis(String code) {
    String c = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(i -> i.name().equals(c))
        .findFirst()
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "INDICATEUR_INCONNU", "Indicateur « " + code + " » inconnu."));
  }
}

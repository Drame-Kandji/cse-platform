package sn.cse.besoins.domain.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sn.cse.besoins.domain.exception.ValidationNonAutoriseeException;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.common.security.Roles;

/**
 * Chaîne de responsabilité des étapes de validation d'un besoin (ADR-0010). Par défaut, une seule
 * étape : le Responsable Matériel (R01). D'autres étapes (ex. Directeur de projet au-delà d'un
 * seuil) se déclarent par configuration, dans l'ordre de franchissement.
 */
public final class ChaineValidation {

  /** Code de l'étape par défaut. */
  public static final String ETAPE_RESPONSABLE_MATERIEL = "RESPONSABLE_MATERIEL";

  private final List<EtapeValidation> etapes;
  private final MaillonValidation tete;

  /**
   * @param etapes étapes actives, dans l'ordre ; la première doit être inconditionnelle afin que
   *     tout besoin ait au moins une étape
   */
  public ChaineValidation(List<EtapeValidation> etapes) {
    if (etapes == null || etapes.isEmpty()) {
      throw new IllegalArgumentException("La chaîne de validation doit comporter une étape");
    }
    if (!etapes.get(0).critere().estInconditionnel()) {
      throw new IllegalArgumentException(
          "La première étape de validation doit s'appliquer à tous les besoins");
    }
    Set<String> codes = new HashSet<>();
    etapes.forEach(
        e -> {
          if (!codes.add(e.code())) {
            throw new IllegalArgumentException("Étape de validation en double : " + e.code());
          }
        });
    this.etapes = List.copyOf(etapes);
    MaillonValidation maillon = null;
    for (int i = this.etapes.size() - 1; i >= 0; i--) {
      maillon = new MaillonValidation(this.etapes.get(i), maillon);
    }
    this.tete = maillon;
  }

  /**
   * @return chaîne par défaut : Responsable Matériel uniquement (R01)
   */
  public static ChaineValidation parDefaut() {
    return new ChaineValidation(
        List.of(
            new EtapeValidation(
                ETAPE_RESPONSABLE_MATERIEL,
                Roles.RESPONSABLE_MATERIEL,
                CritereDeclenchement.toujours())));
  }

  /**
   * Fait franchir au besoin l'étape en attente.
   *
   * @param besoin besoin en attente
   * @param validateur auteur de la validation
   * @return la décision (finale ou intermédiaire)
   * @throws ValidationNonAutoriseeException si le validateur n'a pas le rôle de l'étape (R01)
   */
  public DecisionValidation traiter(BesoinMateriel besoin, Acteur validateur) {
    return tete.traiter(besoin, validateur);
  }

  /**
   * @param besoin besoin évalué
   * @return l'étape en attente, vide si toutes les étapes requises sont franchies
   */
  public Optional<EtapeValidation> etapeEnAttente(BesoinMateriel besoin) {
    return tete.premiereEtapeEnAttente(besoin);
  }

  /**
   * Règle R01 : le rejet appartient au détenteur du rôle de l'étape en attente.
   *
   * @param besoin besoin à rejeter
   * @param acteur auteur du rejet
   * @throws ValidationNonAutoriseeException si l'acteur n'est pas habilité
   */
  public void verifierAutoriteDeRejet(BesoinMateriel besoin, Acteur acteur) {
    EtapeValidation etape = etapeEnAttente(besoin).orElse(etapes.get(0));
    if (!acteur.aLeRole(etape.roleRequis())) {
      throw new ValidationNonAutoriseeException(etape.code(), etape.roleRequis());
    }
  }

  /**
   * @return étapes actives dans l'ordre
   */
  public List<EtapeValidation> etapes() {
    return etapes;
  }
}

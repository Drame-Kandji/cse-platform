package sn.cse.besoins.domain.validation;

import java.util.Optional;
import sn.cse.besoins.domain.exception.ValidationNonAutoriseeException;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;

/**
 * Maillon de la chaîne de responsabilité : traite la validation si son étape est en attente pour le
 * besoin, sinon délègue au maillon suivant.
 */
final class MaillonValidation {

  private final EtapeValidation etape;
  private final MaillonValidation suivant;

  MaillonValidation(EtapeValidation etape, MaillonValidation suivant) {
    this.etape = etape;
    this.suivant = suivant;
  }

  DecisionValidation traiter(BesoinMateriel besoin, Acteur validateur) {
    if (!estEnAttente(besoin)) {
      if (suivant == null) {
        throw new IllegalStateException(
            "Aucune étape de validation en attente pour " + besoin.reference());
      }
      return suivant.traiter(besoin, validateur);
    }
    // Règle R01 : seul le rôle habilité de l'étape courante peut la franchir.
    if (!validateur.aLeRole(etape.roleRequis())) {
      throw new ValidationNonAutoriseeException(etape.code(), etape.roleRequis());
    }
    return new DecisionValidation(
        etape, suivant == null ? Optional.empty() : suivant.premiereEtapeEnAttente(besoin));
  }

  Optional<EtapeValidation> premiereEtapeEnAttente(BesoinMateriel besoin) {
    if (estEnAttente(besoin)) {
      return Optional.of(etape);
    }
    return suivant == null ? Optional.empty() : suivant.premiereEtapeEnAttente(besoin);
  }

  private boolean estEnAttente(BesoinMateriel besoin) {
    return etape.estRequisePour(besoin) && !besoin.aFranchiEtape(etape.code());
  }
}

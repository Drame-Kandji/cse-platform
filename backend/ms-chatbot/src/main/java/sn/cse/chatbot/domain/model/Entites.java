package sn.cse.chatbot.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Entités extraites d'une question.
 *
 * @param referencesBesoin références {@code BES-2026-0012}
 * @param referencesContrat références {@code CTR-2026-003}
 * @param referencesAppelOffre références {@code AO-2026-001}
 * @param codesEngins codes ou immatriculations de matériels
 * @param categorie catégorie de matériel ({@code CategorieMateriel}) ou {@code null}
 * @param termeMateriel désignation recherchée (ex. « niveleuse ») ou {@code null}
 * @param chantier nom ou code de chantier mentionné ou {@code null}
 * @param mesChantiers vrai si l'utilisateur parle de « son » chantier
 * @param quantite quantité demandée ou {@code null}
 * @param statutBesoin statut de besoin filtré ou {@code null}
 */
public record Entites(
    List<String> referencesBesoin,
    List<String> referencesContrat,
    List<String> referencesAppelOffre,
    List<String> codesEngins,
    String categorie,
    String termeMateriel,
    String chantier,
    boolean mesChantiers,
    Integer quantite,
    String statutBesoin) {

  public Entites {
    referencesBesoin = List.copyOf(referencesBesoin);
    referencesContrat = List.copyOf(referencesContrat);
    referencesAppelOffre = List.copyOf(referencesAppelOffre);
    codesEngins = List.copyOf(codesEngins);
  }

  /**
   * @return le premier code d'engin, s'il existe
   */
  public Optional<String> codeEngin() {
    return codesEngins.stream().findFirst();
  }

  /**
   * @return le terme de recherche de matériel : code d'engin en priorité, sinon désignation
   */
  public Optional<String> rechercheMateriel() {
    return codeEngin().or(() -> Optional.ofNullable(termeMateriel));
  }
}

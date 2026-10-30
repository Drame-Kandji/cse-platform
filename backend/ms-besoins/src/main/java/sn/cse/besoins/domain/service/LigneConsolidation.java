package sn.cse.besoins.domain.service;

import java.util.List;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Regroupement de besoins équivalents (même catégorie, même désignation normalisée).
 *
 * @param categorie catégorie
 * @param designation désignation représentative (la plus fréquente)
 * @param quantiteTotale somme des quantités
 * @param nombreDemandes nombre de besoins regroupés
 * @param prioriteMax priorité la plus élevée
 * @param chantiers chantiers concernés
 * @param besoinIds besoins regroupés
 * @param doublonPotentiel vrai si un chantier a exprimé plusieurs fois ce besoin dans la fenêtre
 */
public record LigneConsolidation(
    CategorieMateriel categorie,
    String designation,
    int quantiteTotale,
    int nombreDemandes,
    Priorite prioriteMax,
    List<ChantierConcerne> chantiers,
    List<UUID> besoinIds,
    boolean doublonPotentiel) {

  /**
   * Chantier concerné par un regroupement.
   *
   * @param id identifiant
   * @param nom nom
   */
  public record ChantierConcerne(UUID id, String nom) {}
}

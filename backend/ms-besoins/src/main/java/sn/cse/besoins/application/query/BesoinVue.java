package sn.cse.besoins.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Représentation de lecture d'un besoin (contrat d'API §3, ressource {@code Besoin}).
 *
 * @param id identifiant
 * @param reference référence lisible
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param statut statut
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param demandeurId demandeur
 * @param demandeurNom nom du demandeur
 * @param dureeUtilisationJours durée d'utilisation
 * @param dateBesoin date de besoin
 * @param justification justification
 * @param motifRejet motif de rejet
 * @param validateurId auteur de la décision
 * @param validateurNom nom de l'auteur de la décision
 * @param dateValidation instant de la décision
 * @param materielIdsAffectes matériels affectés
 * @param orientationEnAttente orientation à relancer
 * @param createdAt instant de soumission
 * @param updatedAt instant de dernière modification
 * @param version version
 */
public record BesoinVue(
    UUID id,
    String reference,
    String designation,
    CategorieMateriel categorie,
    UUID typeEnginId,
    int quantite,
    String specifications,
    Priorite priorite,
    StatutBesoin statut,
    UUID chantierId,
    String chantierNom,
    UUID demandeurId,
    String demandeurNom,
    int dureeUtilisationJours,
    LocalDate dateBesoin,
    String justification,
    String motifRejet,
    UUID validateurId,
    String validateurNom,
    Instant dateValidation,
    List<UUID> materielIdsAffectes,
    boolean orientationEnAttente,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public BesoinVue {
    materielIdsAffectes =
        materielIdsAffectes == null ? List.of() : List.copyOf(materielIdsAffectes);
  }

  /**
   * @param b agrégat
   * @return la vue correspondante
   */
  public static BesoinVue depuis(BesoinMateriel b) {
    DescriptionBesoin d = b.description();
    return new BesoinVue(
        b.id(),
        b.reference(),
        d.designation(),
        d.categorie(),
        d.typeEnginId(),
        d.quantite(),
        d.specifications(),
        d.priorite(),
        b.statut(),
        b.chantierId(),
        b.chantierNom(),
        b.demandeurId(),
        b.demandeurNom(),
        d.dureeUtilisationJours(),
        d.dateBesoin(),
        d.justification(),
        b.motifRejet(),
        b.validateurId(),
        b.validateurNom(),
        b.dateValidation(),
        b.materielIdsAffectes(),
        b.orientationEnAttente(),
        b.creeLe(),
        b.modifieLe(),
        b.version() == null ? 0L : b.version());
  }
}

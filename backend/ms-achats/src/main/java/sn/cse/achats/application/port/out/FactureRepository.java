package sn.cse.achats.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Port de persistance des factures fournisseurs. */
public interface FactureRepository {

  /** Champs triables des listes de factures. */
  Set<String> TRIABLES = Set.of("dateEmission", "dateEcheance", "numero", "statut");

  /**
   * Critères de recherche.
   *
   * @param statut statut (facultatif)
   * @param fournisseurId fournisseur (facultatif)
   * @param contratId contrat (facultatif)
   * @param enRetard vrai : seulement les factures en retard ; faux : seulement les autres
   */
  record Filtre(StatutFacture statut, UUID fournisseurId, UUID contratId, Boolean enRetard) {}

  /**
   * @param facture agrégat à enregistrer
   * @return l'agrégat enregistré
   */
  Facture enregistrer(Facture facture);

  /**
   * @param id identifiant
   * @return la facture si elle existe
   */
  Optional<Facture> parId(UUID id);

  /**
   * @param filtre critères
   * @param aujourdhui date de référence du retard
   * @param page pagination
   * @return la page de factures
   */
  PageResponse<Facture> rechercher(Filtre filtre, LocalDate aujourdhui, PageQuery page);

  /**
   * @param contratId contrat
   * @return les factures du contrat
   */
  List<Facture> parContrat(UUID contratId);

  /**
   * @param fournisseurId fournisseur
   * @param numero numéro de facture
   * @return vrai si ce fournisseur a déjà une facture portant ce numéro
   */
  boolean existe(UUID fournisseurId, String numero);

  /**
   * @return toutes les factures (synthèse financière)
   */
  List<Facture> toutes();
}

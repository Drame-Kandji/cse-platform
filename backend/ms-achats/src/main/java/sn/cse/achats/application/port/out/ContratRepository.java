package sn.cse.achats.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Port de persistance des contrats d'achat (et de leurs engagements). */
public interface ContratRepository {

  /** Champs triables des listes de contrats. */
  Set<String> TRIABLES =
      Set.of("numero", "dateSignature", "dateLivraisonPrevue", "statut", "createdAt");

  /** Champs triables des listes d'engagements. */
  Set<String> TRIABLES_ENGAGEMENTS = Set.of("dateDemande", "dateDecision");

  /**
   * Critères de recherche.
   *
   * @param statut statut (facultatif)
   * @param fournisseurId titulaire (facultatif)
   * @param appelOffreId appel d'offres d'origine (facultatif)
   * @param q texte libre sur le numéro ou l'objet (facultatif)
   */
  record Filtre(StatutContrat statut, UUID fournisseurId, UUID appelOffreId, String q) {}

  /**
   * @param contrat agrégat à enregistrer
   * @return l'agrégat enregistré
   */
  ContratAchat enregistrer(ContratAchat contrat);

  /**
   * @param id identifiant
   * @return le contrat s'il existe
   */
  Optional<ContratAchat> parId(UUID id);

  /**
   * @param appelOffreId appel d'offres
   * @return le contrat issu de cet appel d'offres, s'il existe
   */
  Optional<ContratAchat> parAppelOffre(UUID appelOffreId);

  /**
   * @param appelOffreIds appels d'offres
   * @return identifiant du contrat généré, indexé par appel d'offres (absents si aucun contrat)
   */
  Map<UUID, UUID> idsParAppelOffre(Collection<UUID> appelOffreIds);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page de contrats
   */
  PageResponse<ContratAchat> rechercher(Filtre filtre, PageQuery page);

  /**
   * @param statut statut d'engagement (facultatif)
   * @param page pagination
   * @return les contrats ayant un engagement (au statut demandé)
   */
  PageResponse<ContratAchat> avecEngagement(StatutApprobation statut, PageQuery page);

  /**
   * @param aujourdhui date du jour
   * @return contrats en exécution dont l'échéance de livraison est dépassée
   */
  List<ContratAchat> enExecutionEcheanceDepassee(LocalDate aujourdhui);

  /**
   * @return tous les contrats (synthèse financière calculée en mémoire, montants chiffrés)
   */
  List<ContratAchat> tous();
}

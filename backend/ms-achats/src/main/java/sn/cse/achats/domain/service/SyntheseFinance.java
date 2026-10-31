package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Predicate;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Engagement;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutFacture;

/**
 * Synthèse financière des achats. Les montants étant chiffrés au repos, les cumuls sont calculés en
 * mémoire et jamais en SQL.
 *
 * @param montantEngage engagements validés par la DAF
 * @param montantEngagementsEnAttente engagements en attente de la DAF
 * @param montantFacture factures non rejetées (TTC)
 * @param montantMandate factures mandatées ou payées (TTC)
 * @param montantPaye factures payées (TTC)
 * @param facturesEnRetard factures non payées dont l'échéance est dépassée
 * @param engagementsEnAttente nombre d'engagements en attente
 */
public record SyntheseFinance(
    BigDecimal montantEngage,
    BigDecimal montantEngagementsEnAttente,
    BigDecimal montantFacture,
    BigDecimal montantMandate,
    BigDecimal montantPaye,
    long facturesEnRetard,
    long engagementsEnAttente) {

  /**
   * @param contrats contrats
   * @param factures factures
   * @param aujourdhui date de référence (retards)
   * @return la synthèse
   */
  public static SyntheseFinance calculer(
      Collection<ContratAchat> contrats, Collection<Facture> factures, LocalDate aujourdhui) {
    return new SyntheseFinance(
        engagements(contrats, StatutApprobation.APPROUVEE),
        engagements(contrats, StatutApprobation.EN_ATTENTE),
        factures(factures, f -> f.getStatut() != StatutFacture.REJETEE),
        factures(
            factures,
            f -> f.getStatut() == StatutFacture.MANDATEE || f.getStatut() == StatutFacture.PAYEE),
        factures(factures, f -> f.getStatut() == StatutFacture.PAYEE),
        factures.stream().filter(f -> f.estEnRetard(aujourdhui)).count(),
        contrats.stream()
            .flatMap(c -> c.getEngagement().stream())
            .filter(e -> e.statut() == StatutApprobation.EN_ATTENTE)
            .count());
  }

  private static BigDecimal engagements(
      Collection<ContratAchat> contrats, StatutApprobation statut) {
    return contrats.stream()
        .flatMap(c -> c.getEngagement().stream())
        .filter(e -> e.statut() == statut)
        .map(Engagement::montant)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal factures(Collection<Facture> factures, Predicate<Facture> filtre) {
    return factures.stream()
        .filter(filtre)
        .map(Facture::getMontantTtc)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}

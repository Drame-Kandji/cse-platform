package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.ConsulterFinanceUseCase;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.service.SyntheseFinance;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures financières (montants déchiffrés et agrégés en mémoire). */
@Service
@Transactional(readOnly = true)
public class ConsultationFinanceService implements ConsulterFinanceUseCase {

  private final FactureRepository factures;
  private final ContratRepository contrats;
  private final Clock clock;

  /**
   * @param factures dépôt des factures
   * @param contrats dépôt des contrats
   * @param clock horloge
   */
  public ConsultationFinanceService(
      FactureRepository factures, ContratRepository contrats, Clock clock) {
    this.factures = factures;
    this.contrats = contrats;
    this.clock = clock;
  }

  @Override
  public PageResponse<Facture> factures(FactureRepository.Filtre filtre, PageQuery page) {
    return factures.rechercher(filtre, LocalDate.now(clock), page);
  }

  @Override
  public SyntheseFinance synthese() {
    return SyntheseFinance.calculer(contrats.tous(), factures.toutes(), LocalDate.now(clock));
  }
}

package sn.cse.achats.application.port.in;

import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.service.SyntheseFinance;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures financières : factures et synthèse. */
public interface ConsulterFinanceUseCase {

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page de factures
   */
  PageResponse<Facture> factures(FactureRepository.Filtre filtre, PageQuery page);

  /**
   * @return la synthèse financière des achats
   */
  SyntheseFinance synthese();
}

package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures sur les contrats et les engagements. */
public interface ConsulterContratsUseCase {

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page de contrats
   */
  PageResponse<ContratAchat> rechercher(ContratRepository.Filtre filtre, PageQuery page);

  /**
   * @param id contrat
   * @return le contrat
   */
  ContratAchat detail(UUID id);

  /**
   * @param statut statut d'engagement (facultatif)
   * @param page pagination
   * @return les contrats portant un engagement
   */
  PageResponse<ContratAchat> engagements(StatutApprobation statut, PageQuery page);
}

package sn.cse.achats.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.ConsulterContratsUseCase;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures sur les contrats et les engagements. */
@Service
@Transactional(readOnly = true)
public class ConsultationContratsService implements ConsulterContratsUseCase {

  private final ContratRepository contrats;

  /**
   * @param contrats dépôt des contrats
   */
  public ConsultationContratsService(ContratRepository contrats) {
    this.contrats = contrats;
  }

  @Override
  public PageResponse<ContratAchat> rechercher(ContratRepository.Filtre filtre, PageQuery page) {
    return contrats.rechercher(filtre, page);
  }

  @Override
  public ContratAchat detail(UUID id) {
    return Chargements.contrat(contrats, id);
  }

  @Override
  public PageResponse<ContratAchat> engagements(StatutApprobation statut, PageQuery page) {
    return contrats.avecEngagement(statut, page);
  }
}

package sn.cse.auth.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.port.in.RechercherUtilisateursUseCase;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.domain.exception.UtilisateurIntrouvableException;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Consultation des comptes (lecture seule). */
@Service
@Transactional(readOnly = true)
public class RechercherUtilisateursService implements RechercherUtilisateursUseCase {

  private final UtilisateurRepository repository;

  /**
   * @param repository profils
   */
  public RechercherUtilisateursService(UtilisateurRepository repository) {
    this.repository = repository;
  }

  @Override
  public PageResponse<Utilisateur> rechercher(CriteresRecherche criteres, PageQuery page) {
    return repository.rechercher(criteres, page);
  }

  @Override
  public Utilisateur parId(UUID id) {
    return repository.parId(id).orElseThrow(() -> new UtilisateurIntrouvableException(id));
  }
}

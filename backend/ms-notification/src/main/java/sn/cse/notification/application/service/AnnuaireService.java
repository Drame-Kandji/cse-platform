package sn.cse.notification.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.notification.application.port.in.SynchroniserAnnuaireUseCase;
import sn.cse.notification.application.port.out.AnnuaireRepository;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Projection locale des utilisateurs de ms-auth. */
@Service
public class AnnuaireService implements SynchroniserAnnuaireUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AnnuaireService.class);

  private final AnnuaireRepository annuaire;

  /**
   * @param annuaire persistance de l'annuaire
   */
  public AnnuaireService(AnnuaireRepository annuaire) {
    this.annuaire = annuaire;
  }

  @Override
  @Transactional
  public boolean synchroniser(UtilisateurAnnuaire utilisateur) {
    boolean plusRecent =
        annuaire.parId(utilisateur.id()).map(e -> e.estRemplaceePar(utilisateur)).orElse(true);
    if (!plusRecent) {
      LOG.debug("Version ancienne de l'utilisateur {} ignorée", utilisateur.id());
      return false;
    }
    annuaire.enregistrer(utilisateur);
    return true;
  }
}

package sn.cse.besoins.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.command.RejeterBesoinCommand;
import sn.cse.besoins.application.port.in.RejeterBesoinUseCase;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.validation.ChaineValidation;

/** Rejet motivé d'un besoin (R01, R08), événement BESOIN_REJETE. */
@Service
public class RejeterBesoinService implements RejeterBesoinUseCase {

  private final EnregistrementBesoin enregistrement;
  private final ChaineValidation chaine;
  private final Clock clock;

  RejeterBesoinService(EnregistrementBesoin enregistrement, ChaineValidation chaine, Clock clock) {
    this.enregistrement = enregistrement;
    this.chaine = chaine;
    this.clock = clock;
  }

  @Override
  @Transactional
  public BesoinVue rejeter(RejeterBesoinCommand commande) {
    BesoinMateriel besoin = enregistrement.charger(commande.besoinId());
    besoin.rejeter(
        chaine, Acteur.depuis(commande.utilisateur()), commande.motif(), Instant.now(clock));
    return BesoinVue.depuis(enregistrement.enregistrerEtPublier(besoin));
  }
}

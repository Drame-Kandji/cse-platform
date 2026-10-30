package sn.cse.besoins.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.command.ModifierBesoinCommand;
import sn.cse.besoins.application.port.in.ModifierBesoinUseCase;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;

/** Modification d'un besoin par son demandeur (R05, R08), événement BESOIN_MODIFIE. */
@Service
public class ModifierBesoinService implements ModifierBesoinUseCase {

  private final VerificationChantier verificationChantier;
  private final EnregistrementBesoin enregistrement;
  private final Clock clock;

  ModifierBesoinService(
      VerificationChantier verificationChantier, EnregistrementBesoin enregistrement, Clock clock) {
    this.verificationChantier = verificationChantier;
    this.enregistrement = enregistrement;
    this.clock = clock;
  }

  @Override
  @Transactional
  public BesoinVue modifier(ModifierBesoinCommand commande) {
    BesoinMateriel besoin = enregistrement.charger(commande.besoinId());
    ControleAccesBesoin.verifierLecture(besoin, commande.utilisateur());
    DescriptionBesoin description = commande.contenu().description();
    UUID chantierId = commande.contenu().chantierId();
    String chantierNom = besoin.chantierNom();
    // Un changement de chantier n'est vérifié que si la modification est encore possible (R05).
    if (!chantierId.equals(besoin.chantierId()) && besoin.statut().estModifiable()) {
      ControleAccesBesoin.verifierChantierAttribue(chantierId, commande.utilisateur());
      chantierNom = verificationChantier.exigerActif(chantierId).nom();
    }
    besoin.modifier(
        commande.version(),
        chantierId,
        chantierNom,
        description,
        Acteur.depuis(commande.utilisateur()),
        Instant.now(clock));
    return BesoinVue.depuis(enregistrement.enregistrerEtPublier(besoin));
  }
}

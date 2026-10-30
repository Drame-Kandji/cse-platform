package sn.cse.besoins.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.command.SoumettreBesoinCommand;
import sn.cse.besoins.application.port.in.SoumettreBesoinUseCase;
import sn.cse.besoins.application.port.out.ChantierPort.ChantierInfo;
import sn.cse.besoins.application.port.out.GenerateurReference;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;

/** Soumission d'un besoin : contrôle du chantier, référence annuelle, événement BESOIN_SOUMIS. */
@Service
public class SoumettreBesoinService implements SoumettreBesoinUseCase {

  private final VerificationChantier verificationChantier;
  private final GenerateurReference generateurReference;
  private final EnregistrementBesoin enregistrement;
  private final Clock clock;

  SoumettreBesoinService(
      VerificationChantier verificationChantier,
      GenerateurReference generateurReference,
      EnregistrementBesoin enregistrement,
      Clock clock) {
    this.verificationChantier = verificationChantier;
    this.generateurReference = generateurReference;
    this.enregistrement = enregistrement;
    this.clock = clock;
  }

  @Override
  @Transactional
  public BesoinVue soumettre(SoumettreBesoinCommand commande) {
    UUID chantierId = commande.contenu().chantierId();
    ControleAccesBesoin.verifierChantierAttribue(chantierId, commande.utilisateur());
    // R08 vérifiée avant tout appel distant.
    DescriptionBesoin description = commande.contenu().description();
    ChantierInfo chantier = verificationChantier.exigerActif(chantierId);
    Instant maintenant = Instant.now(clock);
    String reference =
        generateurReference.prochaineReference(
            LocalDate.ofInstant(maintenant, ZoneOffset.UTC).getYear());
    BesoinMateriel besoin =
        BesoinMateriel.soumettre(
            UUID.randomUUID(),
            reference,
            chantierId,
            chantier.nom(),
            Acteur.depuis(commande.utilisateur()),
            description,
            maintenant);
    return BesoinVue.depuis(enregistrement.enregistrerEtPublier(besoin));
  }
}

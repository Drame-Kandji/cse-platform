package sn.cse.besoins.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import sn.cse.besoins.application.command.OrienterBesoinCommand;
import sn.cse.besoins.application.command.ValiderBesoinCommand;
import sn.cse.besoins.application.port.in.OrienterBesoinUseCase;
import sn.cse.besoins.application.port.in.ValiderBesoinUseCase;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.validation.ChaineValidation;

/**
 * Validation d'un besoin puis orientation automatique. La validation est validée dans sa propre
 * transaction avant tout appel à ms-suivi : une panne pendant l'orientation laisse un besoin {@code
 * VALIDE} relançable, jamais une validation perdue.
 */
@Service
public class ValiderBesoinService implements ValiderBesoinUseCase {

  private final EnregistrementBesoin enregistrement;
  private final ChaineValidation chaine;
  private final OrienterBesoinUseCase orientation;
  private final TransactionOperations transactions;
  private final Clock clock;

  ValiderBesoinService(
      EnregistrementBesoin enregistrement,
      ChaineValidation chaine,
      OrienterBesoinUseCase orientation,
      TransactionOperations transactions,
      Clock clock) {
    this.enregistrement = enregistrement;
    this.chaine = chaine;
    this.orientation = orientation;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Override
  public BesoinVue valider(ValiderBesoinCommand commande) {
    Acteur validateur = Acteur.depuis(commande.utilisateur());
    BesoinMateriel besoin =
        transactions.execute(
            statut -> {
              BesoinMateriel b = enregistrement.charger(commande.besoinId());
              b.valider(chaine, validateur, commande.commentaire(), Instant.now(clock));
              return enregistrement.enregistrerEtPublier(b);
            });
    if (besoin.statut() != StatutBesoin.VALIDE) {
      // Étape intermédiaire de la chaîne : le besoin attend l'étape suivante.
      return BesoinVue.depuis(besoin);
    }
    return orientation.orienter(
        new OrienterBesoinCommand(
            commande.besoinId(), commande.materielIds(), commande.utilisateur()));
  }
}

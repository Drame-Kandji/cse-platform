package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.domain.model.Facture;

/** Traitement des factures fournisseurs. */
public interface GererFacturesUseCase {

  /**
   * @param commande facture reçue
   * @return la facture RECUE
   */
  Facture recevoir(RecevoirFactureCommand commande);

  /**
   * @param factureId facture
   * @param referenceMandat référence du mandat
   * @return la facture MANDATEE
   */
  Facture mandater(UUID factureId, String referenceMandat);

  /**
   * @param factureId facture
   * @return la facture PAYEE
   */
  Facture payer(UUID factureId);

  /**
   * @param factureId facture
   * @param motif motif de rejet
   * @return la facture REJETEE
   */
  Facture rejeter(UUID factureId, String motif);
}

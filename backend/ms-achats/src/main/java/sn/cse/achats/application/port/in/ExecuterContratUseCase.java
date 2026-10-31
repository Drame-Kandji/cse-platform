package sn.cse.achats.application.port.in;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.application.command.ConfirmerLivraisonCommand;
import sn.cse.achats.application.command.ModifierClausesCommand;
import sn.cse.achats.domain.model.ContratAchat;

/** Exécution d'un contrat d'achat, de la rédaction à la clôture. */
public interface ExecuterContratUseCase {

  /**
   * @param commande clauses et jalons
   * @return le contrat
   */
  ContratAchat modifierClauses(ModifierClausesCommand commande);

  /**
   * @param contratId contrat
   * @return le contrat signé (engagement demandé à la DAF)
   */
  ContratAchat signer(UUID contratId);

  /**
   * @param contratId contrat
   * @return le contrat en livraison (R06)
   */
  ContratAchat demarrerLivraison(UUID contratId);

  /**
   * @param contratId contrat
   * @param jalonId jalon
   * @param dateReelle date de réalisation
   * @return le contrat
   */
  ContratAchat enregistrerJalon(UUID contratId, UUID jalonId, LocalDate dateReelle);

  /**
   * @param commande livraison
   * @return le contrat livré
   */
  ContratAchat confirmerLivraison(ConfirmerLivraisonCommand commande);

  /**
   * @param contratId contrat
   * @return le contrat clôturé
   */
  ContratAchat cloturer(UUID contratId);
}

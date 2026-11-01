package sn.cse.achats.application.port.in;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.application.command.ConfirmerLivraisonCommand;
import sn.cse.achats.application.command.ModifierClausesCommand;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.common.security.CurrentUser;

/** Exécution d'un contrat d'achat, de la rédaction à la clôture. */
public interface ExecuterContratUseCase {

  /**
   * @param commande clauses et jalons
   * @return le contrat
   */
  ContratAchat modifierClauses(ModifierClausesCommand commande);

  /**
   * Signature du CSE : le contrat est soumis à la signature du fournisseur (portail).
   *
   * @param contratId contrat BROUILLON
   * @param signataire responsable des achats
   * @return le contrat EN_ATTENTE_SIGNATURE (clauses figées)
   */
  ContratAchat signer(UUID contratId, CurrentUser signataire);

  /**
   * Démarrage de la livraison par le RA, en secours « hors portail » (le cas nominal est
   * l'expédition déclarée par le fournisseur).
   *
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

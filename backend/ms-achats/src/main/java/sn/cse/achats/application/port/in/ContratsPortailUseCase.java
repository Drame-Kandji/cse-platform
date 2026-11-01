package sn.cse.achats.application.port.in;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/**
 * Contrats vus et exécutés par le fournisseur sur le portail : signature ou refus, déclaration
 * d'expédition, jalons. Un contrat d'un autre fournisseur est traité comme inexistant (404).
 */
public interface ContratsPortailUseCase {

  /**
   * @param fournisseur utilisateur du portail
   * @param statut filtre facultatif
   * @param page pagination
   * @return les contrats du fournisseur
   */
  PageResponse<ContratAchat> contrats(
      CurrentUser fournisseur, StatutContrat statut, PageQuery page);

  /**
   * @param fournisseur utilisateur du portail
   * @param contratId contrat
   * @return le contrat (404 s'il n'appartient pas au fournisseur)
   */
  ContratAchat contrat(CurrentUser fournisseur, UUID contratId);

  /**
   * @param fournisseur utilisateur du portail
   * @param contratId contrat EN_ATTENTE_SIGNATURE
   * @return le contrat SIGNE, engagement demandé à la DAF
   */
  ContratAchat signer(CurrentUser fournisseur, UUID contratId);

  /**
   * @param fournisseur utilisateur du portail
   * @param contratId contrat EN_ATTENTE_SIGNATURE
   * @param motif motif du refus
   * @return le contrat revenu en BROUILLON
   */
  ContratAchat refuser(CurrentUser fournisseur, UUID contratId, String motif);

  /**
   * @param fournisseur utilisateur du portail
   * @param contratId contrat ENGAGEMENT_VALIDE (R06)
   * @param dateExpedition date d'expédition
   * @param commentaire commentaire facultatif
   * @return le contrat EN_LIVRAISON
   */
  ContratAchat expedier(
      CurrentUser fournisseur, UUID contratId, LocalDate dateExpedition, String commentaire);

  /**
   * @param fournisseur utilisateur du portail
   * @param contratId contrat EN_LIVRAISON
   * @param jalonId jalon
   * @param dateReelle date de réalisation
   * @return le contrat
   */
  ContratAchat enregistrerJalon(
      CurrentUser fournisseur, UUID contratId, UUID jalonId, LocalDate dateReelle);
}

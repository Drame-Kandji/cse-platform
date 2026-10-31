package sn.cse.achats.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/** Portail fournisseur : toutes les données sont filtrées sur le fournisseur du jeton. */
public interface PortailFournisseurUseCase {

  /**
   * @param fournisseur utilisateur du portail
   * @param page pagination
   * @return appels d'offres publiés ouverts à ce fournisseur
   */
  PageResponse<AppelOffre> appelsOffresOuverts(CurrentUser fournisseur, PageQuery page);

  /**
   * @param fournisseur utilisateur du portail
   * @param appelOffreId appel d'offres
   * @param montant montant proposé
   * @param delaiLivraisonJours délai
   * @param garantieMois garantie
   * @param description description
   * @return l'offre déposée
   */
  OffreFournisseur deposerOffre(
      CurrentUser fournisseur,
      UUID appelOffreId,
      BigDecimal montant,
      int delaiLivraisonJours,
      Integer garantieMois,
      String description);

  /**
   * @param fournisseur utilisateur du portail
   * @param page pagination
   * @return ses offres
   */
  PageResponse<OffreFournisseur> mesOffres(CurrentUser fournisseur, PageQuery page);

  /**
   * @param fournisseur utilisateur du portail
   * @param page pagination
   * @return ses contrats (marchés attribués)
   */
  PageResponse<ContratAchat> attributions(CurrentUser fournisseur, PageQuery page);

  /**
   * @param fournisseur utilisateur du portail
   * @param page pagination
   * @return ses factures
   */
  PageResponse<Facture> factures(CurrentUser fournisseur, PageQuery page);

  /**
   * @param commande facture déposée (contrat du fournisseur uniquement)
   * @return la facture RECUE
   */
  Facture deposerFacture(RecevoirFactureCommand commande);
}

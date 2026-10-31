package sn.cse.achats.application.command;

import java.math.BigDecimal;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Dépôt d'une offre (saisie interne par le RA ou dépôt depuis le portail fournisseur).
 *
 * @param appelOffreId appel d'offres
 * @param fournisseurId fournisseur soumissionnaire
 * @param montantPropose montant (FCFA)
 * @param delaiLivraisonJours délai de livraison
 * @param garantieMois garantie (facultatif)
 * @param description description (facultatif)
 * @param auteur utilisateur à l'origine du dépôt
 */
public record DeposerOffreCommand(
    UUID appelOffreId,
    UUID fournisseurId,
    BigDecimal montantPropose,
    int delaiLivraisonJours,
    Integer garantieMois,
    String description,
    CurrentUser auteur) {}

package sn.cse.achats.application.command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Confirmation de livraison.
 *
 * @param contratId contrat
 * @param dateLivraisonReelle date de livraison
 * @param numeroBonLivraison numéro du bon de livraison
 * @param observations observations
 */
public record ConfirmerLivraisonCommand(
    UUID contratId,
    LocalDate dateLivraisonReelle,
    String numeroBonLivraison,
    String observations) {}

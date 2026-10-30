package sn.cse.fournisseurs.application.command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Livraison constatée par ms-achats ({@code LIVRAISON_EFFECTUEE}).
 *
 * @param fournisseurId fournisseur livreur
 * @param numeroContrat numéro du contrat (traçabilité)
 * @param dateLivraisonPrevue date prévue
 * @param dateLivraisonReelle date réelle
 */
public record LivraisonCommand(
    UUID fournisseurId,
    String numeroContrat,
    LocalDate dateLivraisonPrevue,
    LocalDate dateLivraisonReelle) {}

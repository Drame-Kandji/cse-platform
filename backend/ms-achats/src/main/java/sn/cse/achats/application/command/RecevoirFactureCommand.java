package sn.cse.achats.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Enregistrement d'une facture reçue.
 *
 * @param contratId contrat facturé
 * @param numero numéro de facture du fournisseur
 * @param montantHt montant hors taxes
 * @param tva montant de TVA
 * @param dateEmission date d'émission
 * @param dateEcheance date d'échéance
 * @param auteur utilisateur (un compte portail ne facture que ses propres contrats)
 */
public record RecevoirFactureCommand(
    UUID contratId,
    String numero,
    BigDecimal montantHt,
    BigDecimal tva,
    LocalDate dateEmission,
    LocalDate dateEcheance,
    CurrentUser auteur) {}

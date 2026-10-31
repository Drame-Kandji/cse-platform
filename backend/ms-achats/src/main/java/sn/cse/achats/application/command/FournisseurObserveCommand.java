package sn.cse.achats.application.command;

import java.time.Instant;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutFournisseur;

/**
 * Changement d'état d'un fournisseur publié par ms-fournisseurs (FOURNISSEUR_*).
 *
 * @param fournisseurId fournisseur
 * @param raisonSociale raison sociale
 * @param statut nouveau statut
 * @param observeLe instant de l'événement
 */
public record FournisseurObserveCommand(
    UUID fournisseurId, String raisonSociale, StatutFournisseur statut, Instant observeLe) {}

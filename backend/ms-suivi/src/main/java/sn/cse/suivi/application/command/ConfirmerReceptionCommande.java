package sn.cse.suivi.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Confirmation de réception d'un matériel sur chantier.
 *
 * @param materielId matériel
 * @param affectationId affectation visée (optionnelle : l'affectation active par défaut)
 * @param observations observations du chef de chantier
 * @param auteur utilisateur
 */
public record ConfirmerReceptionCommande(
    UUID materielId, UUID affectationId, String observations, CurrentUser auteur) {}

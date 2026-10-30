package sn.cse.suivi.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.domain.model.GravitePanne;

/**
 * Signalement d'une panne.
 *
 * @param materielId matériel
 * @param gravite gravité
 * @param description symptômes
 * @param auteur utilisateur
 */
public record SignalerPanneCommande(
    UUID materielId, GravitePanne gravite, String description, CurrentUser auteur) {}

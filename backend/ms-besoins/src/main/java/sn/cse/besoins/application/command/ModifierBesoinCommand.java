package sn.cse.besoins.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Modification d'un besoin en attente.
 *
 * @param besoinId besoin
 * @param version version connue du client
 * @param contenu nouveau contenu
 * @param utilisateur auteur
 */
public record ModifierBesoinCommand(
    UUID besoinId, Long version, ContenuBesoin contenu, CurrentUser utilisateur) {}

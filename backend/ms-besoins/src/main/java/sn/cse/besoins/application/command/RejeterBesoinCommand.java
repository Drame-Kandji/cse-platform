package sn.cse.besoins.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Rejet d'un besoin.
 *
 * @param besoinId besoin
 * @param motif motif (obligatoire, R08)
 * @param utilisateur auteur
 */
public record RejeterBesoinCommand(UUID besoinId, String motif, CurrentUser utilisateur) {}

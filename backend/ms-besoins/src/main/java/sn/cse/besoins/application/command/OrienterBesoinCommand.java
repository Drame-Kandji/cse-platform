package sn.cse.besoins.application.command;

import java.util.List;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Orientation (ou relance) d'un besoin validé.
 *
 * @param besoinId besoin
 * @param materielIds matériels à affecter en priorité (optionnel)
 * @param utilisateur auteur (son JWT est relayé à ms-suivi)
 */
public record OrienterBesoinCommand(
    UUID besoinId, List<UUID> materielIds, CurrentUser utilisateur) {

  public OrienterBesoinCommand {
    materielIds = materielIds == null ? List.of() : List.copyOf(materielIds);
  }
}

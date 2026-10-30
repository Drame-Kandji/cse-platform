package sn.cse.besoins.application.command;

import java.util.List;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Validation d'un besoin.
 *
 * @param besoinId besoin
 * @param commentaire commentaire optionnel
 * @param materielIds matériels à affecter en priorité (optionnel)
 * @param utilisateur validateur
 */
public record ValiderBesoinCommand(
    UUID besoinId, String commentaire, List<UUID> materielIds, CurrentUser utilisateur) {

  public ValiderBesoinCommand {
    materielIds = materielIds == null ? List.of() : List.copyOf(materielIds);
  }
}

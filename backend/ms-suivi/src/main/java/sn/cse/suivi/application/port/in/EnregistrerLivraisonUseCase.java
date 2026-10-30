package sn.cse.suivi.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.suivi.application.command.LivraisonRecue;

/** Entrée en parc des matériels livrés par un marché ({@code LIVRAISON_EFFECTUEE}). */
public interface EnregistrerLivraisonUseCase {

  /**
   * Crée {@code quantite} matériels et, si le chantier du besoin est actif, leurs affectations
   * {@code EN_ATTENTE_RECEPTION}.
   *
   * @param livraison livraison reçue
   * @return identifiants des matériels créés
   */
  List<UUID> enregistrer(LivraisonRecue livraison);
}

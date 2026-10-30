package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.domain.model.DonneesChantier;

/** Création, modification et clôture des chantiers. */
public interface GererChantiersUseCase {

  /**
   * @param donnees données du chantier
   * @param auteur utilisateur
   * @return le chantier créé ({@code CHANTIER_CREE})
   */
  ChantierVue creer(DonneesChantier donnees, CurrentUser auteur);

  /**
   * @param id chantier
   * @param donnees nouvelles données
   * @param version version lue par le client
   * @param auteur utilisateur
   * @return le chantier modifié ({@code CHANTIER_MODIFIE})
   */
  ChantierVue modifier(UUID id, DonneesChantier donnees, long version, CurrentUser auteur);

  /**
   * @param id chantier
   * @param auteur utilisateur
   * @return le chantier terminé (refus 422 si des matériels y sont encore affectés)
   */
  ChantierVue terminer(UUID id, CurrentUser auteur);
}

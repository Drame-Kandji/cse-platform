package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.DescriptionMateriel;

/** Enregistrement et modification des fiches matériel. */
public interface GererMaterielsUseCase {

  /**
   * @param description caractéristiques
   * @param heuresFonctionnement compteur initial (optionnel, 0 par défaut)
   * @param auteur utilisateur
   * @return le matériel enregistré ({@code MATERIEL_ENREGISTRE})
   */
  MaterielVue enregistrer(
      DescriptionMateriel description, Long heuresFonctionnement, CurrentUser auteur);

  /**
   * @param id matériel
   * @param description nouvelles caractéristiques
   * @param version version lue par le client
   * @param auteur utilisateur
   * @return le matériel modifié
   */
  MaterielVue modifier(UUID id, DescriptionMateriel description, long version, CurrentUser auteur);
}

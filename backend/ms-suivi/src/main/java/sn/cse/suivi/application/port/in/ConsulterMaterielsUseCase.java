package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaterielDetailVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/** Consultation du parc. */
public interface ConsulterMaterielsUseCase {

  /**
   * Critères de la liste des matériels.
   *
   * @param etat état
   * @param categorie catégorie
   * @param chantierId chantier courant
   * @param q texte libre
   * @param maintenanceDue échéance de maintenance atteinte
   */
  record Criteres(
      EtatMateriel etat,
      CategorieMateriel categorie,
      UUID chantierId,
      String q,
      Boolean maintenanceDue) {}

  /**
   * @param criteres critères
   * @param page pagination
   * @param lecteur utilisateur (chef : matériels de ses chantiers)
   * @return la page de matériels
   */
  PageResponse<MaterielVue> rechercher(Criteres criteres, PageQuery page, CurrentUser lecteur);

  /**
   * @param id matériel
   * @param lecteur utilisateur
   * @return la fiche complète avec historiques
   */
  MaterielDetailVue detail(UUID id, CurrentUser lecteur);
}

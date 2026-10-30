package sn.cse.suivi.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.StatutAffectation;

/** Consultation des affectations et du planning. */
public interface ConsulterAffectationsUseCase {

  /**
   * @param chantierId chantier (optionnel)
   * @param materielId matériel (optionnel)
   * @param statut statut (optionnel)
   * @param page pagination
   * @param lecteur utilisateur (chef : ses chantiers)
   * @return la page d'affectations
   */
  PageResponse<AffectationVue> rechercher(
      UUID chantierId,
      UUID materielId,
      StatutAffectation statut,
      PageQuery page,
      CurrentUser lecteur);

  /**
   * @param chantierId chantier (optionnel)
   * @param categorie catégorie (optionnelle)
   * @param du début (optionnel)
   * @param au fin (optionnelle)
   * @param lecteur utilisateur
   * @return affectations chevauchant la période, par date de début
   */
  List<AffectationVue> planning(
      UUID chantierId,
      CategorieMateriel categorie,
      LocalDate du,
      LocalDate au,
      CurrentUser lecteur);
}

package sn.cse.suivi.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.FiltreMateriels;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;

/** Stockage des matériels. */
public interface MaterielRepository {

  /**
   * @param id identifiant
   * @return le matériel s'il existe
   */
  Optional<Materiel> findById(UUID id);

  /**
   * @param materiel matériel à enregistrer
   * @return l'état enregistré (version à jour)
   */
  Materiel save(Materiel materiel);

  /**
   * @param code code parc
   * @param exclureId matériel à ignorer, peut être {@code null}
   * @return vrai si un autre matériel porte ce code
   */
  boolean existsByCode(String code, UUID exclureId);

  /**
   * @param filtre critères
   * @param page pagination
   * @param aujourdhui date de référence du filtre « maintenance due »
   * @return la page de matériels
   */
  PageResponse<Materiel> rechercher(FiltreMateriels filtre, PageQuery page, LocalDate aujourdhui);

  /**
   * @param ids identifiants
   * @return les matériels trouvés
   */
  List<Materiel> findAllById(Collection<UUID> ids);

  /**
   * @param categorie catégorie (optionnelle)
   * @param designation fragment de désignation (optionnel)
   * @param limite nombre maximal de résultats
   * @return matériels {@code DISPONIBLE} sans demande de réforme, par code croissant
   */
  List<Materiel> rechercherDisponibles(CategorieMateriel categorie, String designation, int limite);

  /**
   * @param categorie catégorie (optionnelle)
   * @param designation fragment de désignation (optionnel)
   * @return nombre total de matériels disponibles correspondants
   */
  long compterDisponibles(CategorieMateriel categorie, String designation);

  /**
   * @param typeEnginId type d'engin
   * @return matériels rattachés à ce type
   */
  List<Materiel> findByTypeEngin(UUID typeEnginId);

  /**
   * @param aujourdhui date du jour
   * @return matériels non réformés dont un seuil de maintenance préventive est atteint
   */
  List<Materiel> findMaintenanceDue(LocalDate aujourdhui);

  /**
   * @param categorie catégorie (optionnelle)
   * @param etat état (optionnel ; à défaut, les réformés sont exclus)
   * @param perimetre chantiers visibles
   * @return matériels géolocalisés
   */
  List<Materiel> findPourCarte(CategorieMateriel categorie, EtatMateriel etat, Perimetre perimetre);

  /**
   * @param chantierIds chantiers
   * @return nombre de matériels présents sur chaque chantier (absents = 0)
   */
  Map<UUID, Long> compterParChantier(Collection<UUID> chantierIds);
}

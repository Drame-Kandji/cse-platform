package sn.cse.fournisseurs.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/** Port de persistance de l'agrégat {@link Fournisseur}. */
public interface FournisseurRepository {

  /**
   * @param id identifiant
   * @return fournisseur s'il existe
   */
  Optional<Fournisseur> parId(UUID id);

  /**
   * Persiste l'agrégat (création ou mise à jour) et le renvoie avec sa nouvelle version.
   *
   * @param fournisseur agrégat
   * @return agrégat persisté
   */
  Fournisseur sauvegarder(Fournisseur fournisseur);

  /**
   * @param ninea NINEA normalisé
   * @param exclu identifiant à exclure (modification), ou {@code null}
   * @return {@code true} si un autre fournisseur utilise ce NINEA
   */
  boolean nineaUtilise(String ninea, UUID exclu);

  /**
   * @param criteres filtres
   * @param page pagination et tri (liste blanche contrôlée en amont)
   * @return page de fournisseurs
   */
  PageResponse<Fournisseur> rechercher(CriteresFournisseurs criteres, PageQuery page);

  /**
   * @param statut statut
   * @return tous les fournisseurs dans ce statut
   */
  List<Fournisseur> parStatut(StatutFournisseur statut);

  /**
   * @param ids identifiants
   * @return fournisseurs trouvés
   */
  List<Fournisseur> parIds(Collection<UUID> ids);

  /**
   * @param aujourdhui date de référence
   * @return fournisseurs possédant au moins un document expiré non encore signalé
   */
  List<Fournisseur> avecDocumentsExpiresNonSignales(LocalDate aujourdhui);

  /**
   * @return nombre de fournisseurs par statut
   */
  Map<StatutFournisseur, Long> compterParStatut();

  /**
   * @param aujourdhui date de référence
   * @return nombre total de documents expirés
   */
  long compterDocumentsExpires(LocalDate aujourdhui);
}

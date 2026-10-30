package sn.cse.besoins.application.query;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Filtres de recherche des besoins (tous optionnels).
 *
 * @param chantierId chantier
 * @param statut statut
 * @param priorite priorité
 * @param categorie catégorie
 * @param q texte recherché dans la référence, la désignation et les spécifications
 * @param du date de soumission minimale (incluse)
 * @param au date de soumission maximale (incluse)
 * @param chantiersAutorises restriction de visibilité ({@code null} = aucune restriction)
 */
public record CriteresRechercheBesoins(
    UUID chantierId,
    StatutBesoin statut,
    Priorite priorite,
    CategorieMateriel categorie,
    String q,
    LocalDate du,
    LocalDate au,
    Set<UUID> chantiersAutorises) {

  public CriteresRechercheBesoins {
    q = q == null || q.isBlank() ? null : q.strip();
    chantiersAutorises = chantiersAutorises == null ? null : Set.copyOf(chantiersAutorises);
  }

  /**
   * @param chantiers chantiers visibles
   * @return les mêmes critères restreints à ces chantiers
   */
  public CriteresRechercheBesoins restreintA(Set<UUID> chantiers) {
    return new CriteresRechercheBesoins(
        chantierId, statut, priorite, categorie, q, du, au, chantiers);
  }
}

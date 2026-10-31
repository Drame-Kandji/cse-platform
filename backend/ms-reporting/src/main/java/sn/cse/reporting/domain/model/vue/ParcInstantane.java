package sn.cse.reporting.domain.model.vue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.EtatMateriel;

/**
 * Photographie quotidienne du parc par catégorie et chantier courant, base de la série {@code
 * TAUX_DISPONIBILITE}.
 *
 * @param jour jour photographié
 * @param categorie catégorie
 * @param chantierId chantier courant ({@code null} : matériel hors chantier)
 * @param total nombre de matériels
 * @param disponibles matériels disponibles
 * @param affectes matériels affectés
 * @param enMaintenance matériels en maintenance
 * @param reformes matériels réformés
 */
public record ParcInstantane(
    LocalDate jour,
    CategorieMateriel categorie,
    UUID chantierId,
    long total,
    long disponibles,
    long affectes,
    long enMaintenance,
    long reformes) {

  public ParcInstantane {
    Objects.requireNonNull(jour, "jour");
    Objects.requireNonNull(categorie, "categorie");
  }

  /**
   * Photographie l'état courant des matériels (ceux dont la catégorie ou l'état sont encore
   * inconnus sont ignorés).
   *
   * @param jour jour photographié
   * @param materiels matériels connus
   * @return une ligne par couple (catégorie, chantier)
   */
  public static List<ParcInstantane> photographier(
      LocalDate jour, Collection<MaterielVue> materiels) {
    record Cle(CategorieMateriel categorie, UUID chantierId) {}
    Map<Cle, long[]> compteurs = new LinkedHashMap<>();
    for (MaterielVue m : materiels) {
      if (m.categorie() == null || m.etat() == null) {
        continue;
      }
      long[] c =
          compteurs.computeIfAbsent(new Cle(m.categorie(), m.chantierId()), k -> new long[5]);
      c[0]++;
      c[1 + m.etat().ordinal()]++;
    }
    return compteurs.entrySet().stream()
        .map(
            e ->
                new ParcInstantane(
                    jour,
                    e.getKey().categorie(),
                    e.getKey().chantierId(),
                    e.getValue()[0],
                    e.getValue()[1 + EtatMateriel.DISPONIBLE.ordinal()],
                    e.getValue()[1 + EtatMateriel.AFFECTE.ordinal()],
                    e.getValue()[1 + EtatMateriel.EN_MAINTENANCE.ordinal()],
                    e.getValue()[1 + EtatMateriel.REFORME.ordinal()]))
        .toList();
  }
}

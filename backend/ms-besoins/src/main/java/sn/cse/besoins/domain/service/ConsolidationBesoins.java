package sn.cse.besoins.domain.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Consolidation des besoins pour le Responsable Matériel : regroupement par catégorie et
 * désignation normalisée, et détection des doublons potentiels (même chantier, même désignation,
 * dates de besoin distantes d'au plus {@code fenetreDoublonJours}).
 */
public final class ConsolidationBesoins {

  private final int fenetreDoublonJours;

  /**
   * @param fenetreDoublonJours fenêtre de détection des doublons, en jours (&gt;= 0)
   */
  public ConsolidationBesoins(int fenetreDoublonJours) {
    if (fenetreDoublonJours < 0) {
      throw new IllegalArgumentException("La fenêtre de doublon doit être positive");
    }
    this.fenetreDoublonJours = fenetreDoublonJours;
  }

  /**
   * @param besoins besoins à consolider
   * @return les regroupements, du plus prioritaire au moins prioritaire puis par quantité
   */
  public List<LigneConsolidation> consolider(Collection<BesoinAConsolider> besoins) {
    Map<Cle, List<BesoinAConsolider>> groupes = new LinkedHashMap<>();
    besoins.forEach(
        b ->
            groupes
                .computeIfAbsent(
                    new Cle(b.categorie(), NormalisationDesignation.normaliser(b.designation())),
                    k -> new ArrayList<>())
                .add(b));
    return groupes.entrySet().stream()
        .map(e -> ligne(e.getKey().categorie(), e.getValue()))
        .sorted(
            Comparator.comparing(LigneConsolidation::prioriteMax)
                .reversed()
                .thenComparing(
                    Comparator.comparingInt(LigneConsolidation::quantiteTotale).reversed())
                .thenComparing(LigneConsolidation::designation))
        .toList();
  }

  private LigneConsolidation ligne(CategorieMateriel categorie, List<BesoinAConsolider> groupe) {
    Priorite max =
        groupe.stream().map(BesoinAConsolider::priorite).reduce(Priorite.NORMAL, Priorite::max);
    Map<UUID, String> chantiers = new LinkedHashMap<>();
    groupe.forEach(b -> chantiers.putIfAbsent(b.chantierId(), b.chantierNom()));
    return new LigneConsolidation(
        categorie,
        designationRepresentative(groupe),
        groupe.stream().mapToInt(BesoinAConsolider::quantite).sum(),
        groupe.size(),
        max,
        chantiers.entrySet().stream()
            .map(c -> new LigneConsolidation.ChantierConcerne(c.getKey(), c.getValue()))
            .toList(),
        groupe.stream().map(BesoinAConsolider::id).toList(),
        contientDoublon(groupe));
  }

  private boolean contientDoublon(List<BesoinAConsolider> groupe) {
    Map<UUID, List<BesoinAConsolider>> parChantier =
        groupe.stream().collect(Collectors.groupingBy(BesoinAConsolider::chantierId));
    return parChantier.values().stream().anyMatch(this::procheDansLeTemps);
  }

  private boolean procheDansLeTemps(List<BesoinAConsolider> memeChantier) {
    List<BesoinAConsolider> tries =
        memeChantier.stream().sorted(Comparator.comparing(BesoinAConsolider::dateBesoin)).toList();
    for (int i = 1; i < tries.size(); i++) {
      long ecart =
          ChronoUnit.DAYS.between(tries.get(i - 1).dateBesoin(), tries.get(i).dateBesoin());
      if (ecart <= fenetreDoublonJours) {
        return true;
      }
    }
    return false;
  }

  private static String designationRepresentative(List<BesoinAConsolider> groupe) {
    Map<String, Long> frequences =
        groupe.stream()
            .map(BesoinAConsolider::designation)
            .filter(Objects::nonNull)
            .collect(
                Collectors.groupingBy(
                    Function.identity(), LinkedHashMap::new, Collectors.counting()));
    return frequences.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("");
  }

  private record Cle(CategorieMateriel categorie, String designationNormalisee) {}
}

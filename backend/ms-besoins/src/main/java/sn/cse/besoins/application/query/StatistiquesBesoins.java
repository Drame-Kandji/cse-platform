package sn.cse.besoins.application.query;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Indicateurs agrégés des besoins.
 *
 * @param total nombre total de besoins
 * @param parStatut répartition par statut (tous les statuts présents, éventuellement à 0)
 * @param parPriorite répartition par priorité (toutes les priorités présentes)
 * @param delaiMoyenValidationHeures délai moyen entre soumission et décision, {@code null} si
 *     aucune décision
 */
public record StatistiquesBesoins(
    long total,
    Map<StatutBesoin, Long> parStatut,
    Map<Priorite, Long> parPriorite,
    Double delaiMoyenValidationHeures) {

  public StatistiquesBesoins {
    parStatut = completer(parStatut, StatutBesoin.class);
    parPriorite = completer(parPriorite, Priorite.class);
  }

  private static <E extends Enum<E>> Map<E, Long> completer(Map<E, Long> source, Class<E> type) {
    EnumMap<E, Long> complet = new EnumMap<>(type);
    for (E valeur : type.getEnumConstants()) {
      complet.put(valeur, source == null ? 0L : source.getOrDefault(valeur, 0L));
    }
    return Collections.unmodifiableMap(complet);
  }
}

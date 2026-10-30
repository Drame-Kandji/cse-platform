package sn.cse.besoins.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ordre de sollicitation des matériels disponibles lors d'une affectation directe : les matériels
 * choisis par le Responsable Matériel passent en premier, les autres disponibles complètent en cas
 * de conflit d'affectation.
 */
public final class SelectionMateriels {

  private SelectionMateriels() {}

  /**
   * @param disponibles matériels annoncés disponibles par ms-suivi
   * @param souhaites matériels choisis par le RM (peut être vide)
   * @param dejaAffectes matériels déjà affectés au besoin (exclus)
   * @return candidats ordonnés, sans doublon
   */
  public static List<UUID> candidats(
      List<UUID> disponibles, List<UUID> souhaites, List<UUID> dejaAffectes) {
    Set<UUID> dispo = new LinkedHashSet<>(disponibles == null ? List.of() : disponibles);
    Set<UUID> ordre = new LinkedHashSet<>();
    if (souhaites != null) {
      souhaites.stream().filter(dispo::contains).forEach(ordre::add);
    }
    ordre.addAll(dispo);
    if (dejaAffectes != null) {
      dejaAffectes.forEach(ordre::remove);
    }
    return new ArrayList<>(ordre);
  }
}

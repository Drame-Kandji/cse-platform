package sn.cse.achats.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Besoin matériel à l'origine d'un appel d'offres (issu de la projection {@link BesoinAAcheter}).
 *
 * @param besoinId identifiant du besoin
 * @param reference référence lisible (BES-2026-0001)
 * @param chantierId chantier demandeur
 * @param chantierNom nom du chantier
 * @param designation désignation du matériel demandé
 */
public record OrigineBesoin(
    UUID besoinId, String reference, UUID chantierId, String chantierNom, String designation) {

  public OrigineBesoin {
    Objects.requireNonNull(besoinId, "besoinId");
  }
}

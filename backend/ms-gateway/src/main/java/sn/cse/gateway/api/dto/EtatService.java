package sn.cse.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * État d'un service, sondé sur son point de santé Actuator.
 *
 * @param nom nom du service
 * @param statut {@code UP} si le service répond « UP » dans le délai, sinon {@code DOWN}
 * @param latenceMs durée de la sonde en millisecondes
 */
@Schema(name = "EtatService", description = "État de santé d'un service")
public record EtatService(
    @Schema(example = "ms-besoins") String nom,
    @Schema(example = "UP") Statut statut,
    @Schema(example = "12") long latenceMs) {

  /** Statut d'un service. */
  public enum Statut {
    UP,
    DOWN
  }
}

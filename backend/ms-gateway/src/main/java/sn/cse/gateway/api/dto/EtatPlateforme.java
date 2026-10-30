package sn.cse.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * État des services de la plateforme.
 *
 * @param services un état par service, dans l'ordre de configuration
 */
@Schema(name = "EtatPlateforme", description = "État de santé des microservices")
public record EtatPlateforme(List<EtatService> services) {

  public EtatPlateforme {
    services = List.copyOf(services);
  }
}

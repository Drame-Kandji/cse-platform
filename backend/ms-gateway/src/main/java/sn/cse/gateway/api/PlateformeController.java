package sn.cse.gateway.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import sn.cse.gateway.api.dto.EtatPlateforme;
import sn.cse.gateway.sante.SondeServices;

/** État de la plateforme pour la page d'administration (rôles internes). */
@RestController
@RequestMapping("/api/plateforme")
@Tag(name = "Plateforme", description = "Supervision des microservices")
public class PlateformeController {

  private final SondeServices sonde;

  /**
   * @param sonde sonde des services
   */
  public PlateformeController(SondeServices sonde) {
    this.sonde = sonde;
  }

  /**
   * @return l'état de chaque service
   */
  @GetMapping("/etat")
  @Operation(
      summary = "État des services",
      description =
          "Interroge en parallèle /actuator/health de chaque service (délai court). Accessible à"
              + " tout rôle interne.")
  @ApiResponse(responseCode = "200", description = "État des services")
  @ApiResponse(
      responseCode = "401",
      description = "Jeton absent ou invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Rôle interne requis",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public Mono<EtatPlateforme> etat() {
    return sonde.sonder();
  }
}

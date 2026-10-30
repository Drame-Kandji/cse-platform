package sn.cse.auth.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.auth.api.dto.UtilisateurDto;
import sn.cse.auth.api.mapper.UtilisateurApiMapper;
import sn.cse.auth.application.command.IdentiteJwt;
import sn.cse.auth.application.port.in.ConsulterMonProfilUseCase;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.CurrentUserProvider;

/** Profil de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Mon profil", description = "Profil de l'utilisateur connecté")
public class MonProfilController {

  private final ConsulterMonProfilUseCase consulter;
  private final UtilisateurApiMapper mapper;

  /**
   * @param consulter cas d'utilisation
   * @param mapper conversion
   */
  public MonProfilController(ConsulterMonProfilUseCase consulter, UtilisateurApiMapper mapper) {
    this.consulter = consulter;
    this.mapper = mapper;
  }

  /**
   * @param jwt jeton de l'utilisateur
   * @return le profil courant
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Mon profil",
      description =
          "Profil de l'utilisateur connecté (identité, rôle, attributs, état du second facteur)."
              + " Créé à la volée depuis le jeton s'il n'a pas encore été synchronisé.")
  @ApiResponse(responseCode = "200", description = "Profil courant")
  @ApiResponse(
      responseCode = "401",
      description = "Jeton absent ou invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Aucun rôle CSE (AUCUN_ROLE_CSE)",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public UtilisateurDto moi(@AuthenticationPrincipal Jwt jwt) {
    CurrentUser courant = CurrentUserProvider.depuisJwt(jwt);
    IdentiteJwt identite =
        new IdentiteJwt(
            UUID.fromString(jwt.getSubject()),
            courant.email(),
            jwt.getClaimAsString("given_name"),
            jwt.getClaimAsString("family_name"),
            courant.roles(),
            courant.chantierIds(),
            courant.fournisseurId());
    return mapper.versDto(consulter.consulter(identite));
  }
}

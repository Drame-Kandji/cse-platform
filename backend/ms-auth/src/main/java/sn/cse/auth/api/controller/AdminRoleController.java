package sn.cse.auth.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.auth.api.dto.RoleInfoDto;
import sn.cse.auth.api.mapper.UtilisateurApiMapper;
import sn.cse.auth.application.port.in.ListerRolesUseCase;

/** Rôles de la plateforme (ADMIN_SYSTEME uniquement). */
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@Tag(name = "Administration des rôles", description = "Rôles, effectifs et exigence d'OTP (ADMIN)")
public class AdminRoleController {

  private final ListerRolesUseCase lister;
  private final UtilisateurApiMapper mapper;

  /**
   * @param lister cas d'utilisation
   * @param mapper conversion
   */
  public AdminRoleController(ListerRolesUseCase lister, UtilisateurApiMapper mapper) {
    this.lister = lister;
    this.mapper = mapper;
  }

  /**
   * @return les rôles
   */
  @GetMapping
  @Operation(summary = "Lister les rôles", description = "Rôles avec libellé, effectif et OTP.")
  @ApiResponse(responseCode = "200", description = "Rôles")
  @ApiResponse(
      responseCode = "401",
      description = "Jeton absent ou invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Rôle ADMIN_SYSTEME requis",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public List<RoleInfoDto> lister() {
    return lister.lister().stream().map(mapper::versDto).toList();
  }
}

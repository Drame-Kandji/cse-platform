package sn.cse.reporting.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.reporting.api.dto.PointDto;
import sn.cse.reporting.api.dto.TableauDto;
import sn.cse.reporting.application.command.DemandeSerie;
import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.application.command.FichierExporte;
import sn.cse.reporting.application.port.in.ConsulterSerieUseCase;
import sn.cse.reporting.application.port.in.ConsulterTableauUseCase;
import sn.cse.reporting.application.port.in.ExporterTableauUseCase;

/**
 * Tableaux de bord, séries et exports (contrat §6). Accès : chef de chantier, responsable matériel,
 * responsable des achats, DAF et Direction Générale ; le choix du profil et le périmètre de
 * chantiers sont contrôlés dans les cas d'utilisation (B.13).
 */
@RestController
@RequestMapping("/api/reporting")
@Tag(name = "Reporting", description = "Tableaux de bord par profil, séries temporelles, exports")
@PreAuthorize(
    "hasAnyRole(T(sn.cse.common.security.Roles).CHEF_CHANTIER,"
        + " T(sn.cse.common.security.Roles).RESPONSABLE_MATERIEL,"
        + " T(sn.cse.common.security.Roles).RESPONSABLE_ACHATS,"
        + " T(sn.cse.common.security.Roles).DAF,"
        + " T(sn.cse.common.security.Roles).DIRECTION_GENERALE)")
@ApiResponse(
    responseCode = "400",
    description = "Paramètre invalide (profil, indicateur, granularité, format, période)",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
    responseCode = "401",
    description = "Jeton absent ou invalide",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
    responseCode = "403",
    description =
        "Rôle sans tableau de bord, profil non autorisé (PROFIL_NON_AUTORISE), indicateur non"
            + " autorisé ou chantier hors périmètre du chef (CHANTIER_NON_AUTORISE)",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
public class ReportingController {

  private static final String DESCRIPTION_PROFIL =
      "Profil : DG (tous les profils lui sont accessibles), DAF, RESPONSABLE_ACHATS,"
          + " RESPONSABLE_MATERIEL, CHEF_CHANTIER. Défaut : profil principal du rôle.";
  private static final String TYPE_XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ConsulterTableauUseCase tableaux;
  private final ConsulterSerieUseCase series;
  private final ExporterTableauUseCase exports;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param tableaux calcul des tableaux de bord
   * @param series calcul des séries
   * @param exports exports PDF / Excel
   * @param utilisateurs utilisateur courant
   */
  public ReportingController(
      ConsulterTableauUseCase tableaux,
      ConsulterSerieUseCase series,
      ExporterTableauUseCase exports,
      CurrentUserProvider utilisateurs) {
    this.tableaux = tableaux;
    this.series = series;
    this.exports = exports;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @return le tableau de bord
   */
  @GetMapping("/kpis")
  @Operation(
      summary = "Tableau de bord d'un profil",
      description =
          "Indicateurs des sections du profil : DG → toutes ; RESPONSABLE_MATERIEL → parc,"
              + " besoins, chantiers ; RESPONSABLE_ACHATS → achats, fournisseurs ; DAF → finances,"
              + " achats, chantiers ; CHEF_CHANTIER → besoins, parc, chantiers limités à ses"
              + " chantiers. Période par défaut : 90 derniers jours.")
  @ApiResponse(responseCode = "200", description = "Tableau de bord")
  public TableauDto kpis(
      @Parameter(description = DESCRIPTION_PROFIL, example = "DG") @RequestParam(required = false)
          String profil,
      @Parameter(description = "Premier jour (défaut : au − 89 jours)", example = "2026-08-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(description = "Dernier jour (défaut : aujourd'hui)", example = "2026-10-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au,
      @Parameter(
              description = "Restreint au chantier (un chef ne peut choisir que l'un des siens)",
              example = "c0000000-0000-0000-0000-000000000001")
          @RequestParam(required = false)
          UUID chantierId) {
    return TableauDto.depuis(tableaux.tableau(demande(profil, du, au, chantierId)));
  }

  /**
   * @return les points de la série
   */
  @GetMapping("/series")
  @Operation(
      summary = "Série temporelle d'un indicateur",
      description =
          "Un point par tranche (JOUR « 2026-07-14 », SEMAINE ISO « 2026-W29 », MOIS « 2026-07 »)."
              + " Pour un cumul (MONTANT_ENGAGE, COUT_MAINTENANCE, BESOINS_SOUMIS) chaque tranche a"
              + " un point, éventuellement 0 ; pour un taux ou une moyenne (TAUX_DISPONIBILITE,"
              + " TAUX_UTILISATION, DELAI_APPROVISIONNEMENT) une tranche sans donnée n'a pas de"
              + " point. L'indicateur doit relever d'une section d'un profil autorisé.")
  @ApiResponse(
      responseCode = "200",
      description = "Points de la série",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = PointDto.class))))
  public List<PointDto> serie(
      @Parameter(
              description = "Indicateur",
              required = true,
              example = "TAUX_DISPONIBILITE",
              schema =
                  @Schema(
                      allowableValues = {
                        "TAUX_DISPONIBILITE",
                        "TAUX_UTILISATION",
                        "MONTANT_ENGAGE",
                        "COUT_MAINTENANCE",
                        "BESOINS_SOUMIS",
                        "DELAI_APPROVISIONNEMENT"
                      }))
          @RequestParam
          String indicateur,
      @Parameter(
              description = "Granularité (défaut MOIS)",
              example = "MOIS",
              schema = @Schema(allowableValues = {"JOUR", "SEMAINE", "MOIS"}))
          @RequestParam(required = false)
          String granularite,
      @Parameter(description = "Premier jour (défaut : au − 89 jours)", example = "2026-08-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(description = "Dernier jour (défaut : aujourd'hui)", example = "2026-10-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au,
      @Parameter(
              description = "Restreint au chantier",
              example = "c0000000-0000-0000-0000-000000000001")
          @RequestParam(required = false)
          UUID chantierId) {
    CurrentUser user = utilisateurs.require();
    return series
        .serie(new DemandeSerie(user, indicateur, granularite, du, au, chantierId))
        .stream()
        .map(PointDto::depuis)
        .toList();
  }

  /**
   * @return le fichier exporté
   */
  @GetMapping("/export")
  @Operation(
      summary = "Exporter le tableau de bord",
      description =
          "Mêmes paramètres et mêmes chiffres que /kpis. PDF : page de titre CSE puis une page par"
              + " section ; Excel : une feuille par section avec formats FCFA et %. Nom proposé :"
              + " tableau-de-bord-<PROFIL>-<aaaa-mm-jj>.<pdf|xlsx>.")
  @ApiResponse(
      responseCode = "200",
      description = "Fichier en pièce jointe",
      headers =
          @Header(
              name = HttpHeaders.CONTENT_DISPOSITION,
              description = "attachment; filename=\"tableau-de-bord-DG-2026-10-30.pdf\"",
              schema = @Schema(type = "string")),
      content = {
        @Content(
            mediaType = MediaType.APPLICATION_PDF_VALUE,
            schema = @Schema(type = "string", format = "binary")),
        @Content(mediaType = TYPE_XLSX, schema = @Schema(type = "string", format = "binary"))
      })
  public ResponseEntity<byte[]> exporter(
      @Parameter(
              description = "Format du fichier",
              required = true,
              example = "pdf",
              schema = @Schema(allowableValues = {"pdf", "xlsx"}))
          @RequestParam
          String format,
      @Parameter(description = DESCRIPTION_PROFIL, example = "DG") @RequestParam(required = false)
          String profil,
      @Parameter(description = "Premier jour (défaut : au − 89 jours)", example = "2026-08-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(description = "Dernier jour (défaut : aujourd'hui)", example = "2026-10-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au,
      @Parameter(
              description = "Restreint au chantier",
              example = "c0000000-0000-0000-0000-000000000001")
          @RequestParam(required = false)
          UUID chantierId) {
    FichierExporte fichier = exports.exporter(demande(profil, du, au, chantierId), format);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fichier.nom()).build().toString())
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(fichier.typeMime()))
        .body(fichier.contenu());
  }

  private DemandeTableau demande(String profil, LocalDate du, LocalDate au, UUID chantierId) {
    return new DemandeTableau(utilisateurs.require(), profil, du, au, chantierId);
  }
}

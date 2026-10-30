package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.api.dto.ClotureMaintenanceRequest;
import sn.cse.suivi.api.dto.MaintenanceDto;
import sn.cse.suivi.api.dto.MaintenanceRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.command.CloturerMaintenanceCommande;
import sn.cse.suivi.application.command.PlanifierMaintenanceCommande;
import sn.cse.suivi.application.port.in.ConsulterMaintenancesUseCase;
import sn.cse.suivi.application.port.in.GererMaintenancesUseCase;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/** Ordres de travail : {@code PLANIFIEE → EN_COURS → CLOTUREE}. */
@RestController
@RequestMapping("/api/maintenances")
@Tag(name = "Maintenances", description = "Ordres de travail de maintenance (OT-AAAA-NNNN)")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant")
public class MaintenanceController {

  /** Champs triables. */
  static final Set<String> TRIABLES = Set.of("datePlanifiee", "numeroOt", "statut", "type");

  private static final String TRI_DEFAUT = "datePlanifiee,desc";

  private final ConsulterMaintenancesUseCase consultation;
  private final GererMaintenancesUseCase gestion;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param consultation consultation des ordres de travail
   * @param gestion cycle de vie des ordres de travail
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   */
  public MaintenanceController(
      ConsulterMaintenancesUseCase consultation,
      GererMaintenancesUseCase gestion,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.consultation = consultation;
    this.gestion = gestion;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param statut statut
   * @param type type
   * @param materielId matériel
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page d'ordres de travail
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Lister les ordres de travail",
      description = "Un chef de chantier ne voit que les interventions de ses chantiers.")
  @ApiResponse(responseCode = "200", description = "Page d'ordres de travail")
  public PageResponse<MaintenanceDto> lister(
      @Parameter(example = "PLANIFIEE") @RequestParam(required = false) StatutMaintenance statut,
      @Parameter(example = "PREVENTIVE") @RequestParam(required = false) TypeMaintenance type,
      @Parameter(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @RequestParam(required = false)
          UUID materielId,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(
              description = "Tri : datePlanifiee, numeroOt, statut, type",
              example = "datePlanifiee,desc")
          @RequestParam(required = false)
          String sort) {
    return consultation
        .rechercher(
            statut,
            type,
            materielId,
            pages.query(page, size, sort, TRIABLES, TRI_DEFAUT),
            utilisateurs.require())
        .map(ApiMapper::maintenance);
  }

  /**
   * @param requete planification
   * @return l'ordre de travail créé
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Planifier un ordre de travail",
      description =
          "Numéro OT-AAAA-NNNN attribué par séquence ; une panne liée passe PRISE_EN_CHARGE ;"
              + " émet MAINTENANCE_PLANIFIEE.")
  @ApiResponse(responseCode = "201", description = "Ordre de travail planifié")
  @ApiResponse(responseCode = "404", description = "Matériel ou panne introuvable")
  @ApiResponse(responseCode = "409", description = "Panne déjà prise en charge")
  @ApiResponse(responseCode = "422", description = "Matériel réformé ou panne d'un autre matériel")
  public MaintenanceDto planifier(@Valid @RequestBody MaintenanceRequest requete) {
    return ApiMapper.maintenance(
        gestion.planifier(
            new PlanifierMaintenanceCommande(
                requete.materielId(),
                new Maintenance.Planification(
                    requete.type(),
                    requete.description(),
                    requete.datePlanifiee(),
                    requete.technicien(),
                    requete.panneId()),
                utilisateurs.require())));
  }

  /**
   * @param id ordre de travail
   * @return l'ordre de travail en cours
   */
  @PatchMapping("/{id}/demarrer")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Démarrer une intervention",
      description = "Le matériel passe EN_MAINTENANCE ; émet MAINTENANCE_DEMARREE.")
  @ApiResponse(responseCode = "200", description = "Ordre de travail en cours")
  @ApiResponse(responseCode = "404", description = "Ordre de travail introuvable")
  @ApiResponse(responseCode = "409", description = "TRANSITION_INVALIDE")
  @ApiResponse(responseCode = "422", description = "Matériel réformé")
  public MaintenanceDto demarrer(
      @Parameter(example = "3e4f5a6b-7c8d-4e9f-0a1b-2c3d4e5f6a7b") @PathVariable UUID id) {
    return ApiMapper.maintenance(gestion.demarrer(id, utilisateurs.require()));
  }

  /**
   * @param id ordre de travail
   * @param requete coût, observations, compteur
   * @return l'ordre de travail clôturé
   */
  @PatchMapping("/{id}/cloturer")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Clôturer une intervention",
      description =
          "Le matériel redevient AFFECTE si une affectation est en cours, sinon DISPONIBLE ; le"
              + " cycle de maintenance préventive repart ; émet MAINTENANCE_CLOTUREE.")
  @ApiResponse(responseCode = "200", description = "Ordre de travail clôturé")
  @ApiResponse(responseCode = "404", description = "Ordre de travail introuvable")
  @ApiResponse(responseCode = "409", description = "TRANSITION_INVALIDE")
  @ApiResponse(responseCode = "422", description = "Coût ou relevé de compteur invalide")
  public MaintenanceDto cloturer(
      @PathVariable UUID id, @Valid @RequestBody ClotureMaintenanceRequest requete) {
    return ApiMapper.maintenance(
        gestion.cloturer(
            new CloturerMaintenanceCommande(
                id,
                requete.cout(),
                requete.observations(),
                requete.heuresCompteur(),
                utilisateurs.require())));
  }
}

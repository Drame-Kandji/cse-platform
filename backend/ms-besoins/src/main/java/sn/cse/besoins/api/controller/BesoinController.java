package sn.cse.besoins.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.cse.besoins.api.dto.BesoinResponse;
import sn.cse.besoins.api.dto.ConsolidationResponse;
import sn.cse.besoins.api.dto.HistoriqueBesoinResponse;
import sn.cse.besoins.api.dto.ModificationBesoinRequest;
import sn.cse.besoins.api.dto.OrientationBesoinRequest;
import sn.cse.besoins.api.dto.RejetBesoinRequest;
import sn.cse.besoins.api.dto.SoumissionBesoinRequest;
import sn.cse.besoins.api.dto.StatistiquesBesoinsResponse;
import sn.cse.besoins.api.dto.ValidationBesoinRequest;
import sn.cse.besoins.application.command.ModifierBesoinCommand;
import sn.cse.besoins.application.command.OrienterBesoinCommand;
import sn.cse.besoins.application.command.RejeterBesoinCommand;
import sn.cse.besoins.application.command.SoumettreBesoinCommand;
import sn.cse.besoins.application.command.ValiderBesoinCommand;
import sn.cse.besoins.application.port.in.ConsoliderBesoinsUseCase;
import sn.cse.besoins.application.port.in.ConsulterBesoinsUseCase;
import sn.cse.besoins.application.port.in.ModifierBesoinUseCase;
import sn.cse.besoins.application.port.in.OrienterBesoinUseCase;
import sn.cse.besoins.application.port.in.RejeterBesoinUseCase;
import sn.cse.besoins.application.port.in.SoumettreBesoinUseCase;
import sn.cse.besoins.application.port.in.StatistiquesBesoinsUseCase;
import sn.cse.besoins.application.port.in.ValiderBesoinUseCase;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.security.Roles;
import sn.cse.common.web.PageableFactory;

/** API REST des besoins en matériel (contrat d'API §3). */
@RestController
@RequestMapping("/api/besoins")
@Tag(
    name = "Besoins",
    description =
        "Expression, validation (R01), rejet motivé (R08) et orientation des besoins en matériel"
            + " des chantiers : affectation directe depuis le parc ou transmission aux achats.")
public class BesoinController {

  static final String PROBLEME = "application/problem+json";

  /** Champs triables (liste blanche). */
  static final Set<String> TRIABLES =
      Set.of(
          "createdAt",
          "updatedAt",
          "dateBesoin",
          "priorite",
          "statut",
          "reference",
          "quantite",
          "designation");

  static final String TRI_PAR_DEFAUT = "createdAt,desc";

  private static final String DEMANDEURS =
      "hasAnyRole('" + Roles.CHEF_CHANTIER + "','" + Roles.RESPONSABLE_MATERIEL + "')";
  private static final String LECTEURS =
      "hasAnyRole('"
          + Roles.CHEF_CHANTIER
          + "','"
          + Roles.RESPONSABLE_MATERIEL
          + "','"
          + Roles.RESPONSABLE_ACHATS
          + "','"
          + Roles.DIRECTION_GENERALE
          + "')";
  private static final String RM = "hasRole('" + Roles.RESPONSABLE_MATERIEL + "')";
  private static final String INTERNES =
      "hasAnyRole('"
          + Roles.CHEF_CHANTIER
          + "','"
          + Roles.RESPONSABLE_MATERIEL
          + "','"
          + Roles.RESPONSABLE_ACHATS
          + "','"
          + Roles.DAF
          + "','"
          + Roles.DIRECTION_GENERALE
          + "','"
          + Roles.ADMIN_SYSTEME
          + "')";

  private final SoumettreBesoinUseCase soumission;
  private final ModifierBesoinUseCase modification;
  private final ValiderBesoinUseCase validation;
  private final RejeterBesoinUseCase rejet;
  private final OrienterBesoinUseCase orientation;
  private final ConsulterBesoinsUseCase consultation;
  private final ConsoliderBesoinsUseCase consolidation;
  private final StatistiquesBesoinsUseCase statistiques;
  private final CurrentUserProvider utilisateurs;
  private final PageableFactory pages;

  /**
   * @param soumission soumission
   * @param modification modification
   * @param validation validation
   * @param rejet rejet
   * @param orientation orientation
   * @param consultation consultation
   * @param consolidation consolidation
   * @param statistiques statistiques
   * @param utilisateurs utilisateur courant
   * @param pages pagination
   */
  public BesoinController(
      SoumettreBesoinUseCase soumission,
      ModifierBesoinUseCase modification,
      ValiderBesoinUseCase validation,
      RejeterBesoinUseCase rejet,
      OrienterBesoinUseCase orientation,
      ConsulterBesoinsUseCase consultation,
      ConsoliderBesoinsUseCase consolidation,
      StatistiquesBesoinsUseCase statistiques,
      CurrentUserProvider utilisateurs,
      PageableFactory pages) {
    this.soumission = soumission;
    this.modification = modification;
    this.validation = validation;
    this.rejet = rejet;
    this.orientation = orientation;
    this.consultation = consultation;
    this.consolidation = consolidation;
    this.statistiques = statistiques;
    this.utilisateurs = utilisateurs;
    this.pages = pages;
  }

  /**
   * @param requete besoin exprimé
   * @return 201 et le besoin créé
   */
  @PostMapping
  @PreAuthorize(DEMANDEURS)
  @Operation(
      summary = "Soumettre un besoin",
      description =
          "Crée un besoin EN_ATTENTE. Un chef de chantier ne peut le rattacher qu'à un de ses"
              + " chantiers ; le chantier doit exister et être EN_COURS (vérifié auprès de"
              + " ms-suivi). Règle R08 : justification obligatoire si la priorité est URGENT ou"
              + " CRITIQUE. Publie BESOIN_SOUMIS.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Besoin créé"),
    @ApiResponse(
        responseCode = "400",
        description = "Champs invalides",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou chantier non attribué (CHANTIER_NON_ATTRIBUE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "422",
        description =
            "JUSTIFICATION_OBLIGATOIRE (R08), CHANTIER_INTROUVABLE, CHANTIER_INACTIF,"
                + " DATE_BESOIN_PASSEE",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "503",
        description = "ms-suivi indisponible (SERVICE_SUIVI_INDISPONIBLE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<BesoinResponse> soumettre(
      @Valid @RequestBody SoumissionBesoinRequest requete) {
    BesoinResponse cree =
        BesoinResponse.depuis(
            soumission.soumettre(new SoumettreBesoinCommand(requete.contenu(), utilisateur())));
    URI emplacement =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(cree.id())
            .toUri();
    return ResponseEntity.created(emplacement).body(cree);
  }

  /**
   * @param id besoin
   * @param requete nouveau contenu et version
   * @return le besoin modifié
   */
  @PutMapping("/{id}")
  @PreAuthorize(DEMANDEURS)
  @Operation(
      summary = "Modifier un besoin",
      description =
          "Réservé au demandeur, uniquement tant que le besoin est EN_ATTENTE (règle R05)."
              + " Publie BESOIN_MODIFIE.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Besoin modifié"),
    @ApiResponse(
        responseCode = "400",
        description = "Champs invalides",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Pas le demandeur (MODIFICATION_RESERVEE_DEMANDEUR) ou chantier non attribué",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "BESOIN_NON_MODIFIABLE (R05) ou CONFLIT_CONCURRENCE",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "422",
        description = "JUSTIFICATION_OBLIGATOIRE (R08), CHANTIER_INACTIF…",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public BesoinResponse modifier(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id,
      @Valid @RequestBody ModificationBesoinRequest requete) {
    return BesoinResponse.depuis(
        modification.modifier(
            new ModifierBesoinCommand(id, requete.version(), requete.contenu(), utilisateur())));
  }

  /**
   * @param id besoin
   * @return le besoin
   */
  @GetMapping("/{id}")
  @PreAuthorize(LECTEURS)
  @Operation(
      summary = "Consulter un besoin",
      description = "Un chef de chantier ne consulte que les besoins de ses chantiers.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Besoin"),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou chantier non attribué (ACCES_CHANTIER_REFUSE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable (BESOIN_INTROUVABLE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public BesoinResponse obtenir(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id) {
    return BesoinResponse.depuis(consultation.obtenir(id, utilisateur()));
  }

  /**
   * @param chantierId filtre chantier
   * @param statut filtre statut
   * @param priorite filtre priorité
   * @param categorie filtre catégorie
   * @param q texte libre
   * @param du soumis à partir de
   * @param au soumis jusqu'au
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page de besoins
   */
  @GetMapping
  @PreAuthorize(LECTEURS)
  @Operation(
      summary = "Rechercher des besoins",
      description =
          "Liste paginée (taille max 50). Un chef de chantier ne voit que ses chantiers. Tri"
              + " autorisé : createdAt, updatedAt, dateBesoin, priorite, statut, reference,"
              + " quantite, designation (défaut : createdAt,desc). Les bornes du/au portent sur la"
              + " date de soumission.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Page de besoins"),
    @ApiResponse(
        responseCode = "400",
        description = "Paramètre invalide ou tri non autorisé (TRI_NON_AUTORISE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou chantier non attribué",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public PageResponse<BesoinResponse> rechercher(
      @Parameter(description = "Chantier", example = "c0000000-0000-0000-0000-000000000001")
          @RequestParam(required = false)
          UUID chantierId,
      @Parameter(description = "Statut", example = "EN_ATTENTE") @RequestParam(required = false)
          StatutBesoin statut,
      @Parameter(description = "Priorité", example = "URGENT") @RequestParam(required = false)
          Priorite priorite,
      @Parameter(description = "Catégorie", example = "MATERIEL_ROULANT")
          @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(
              description = "Texte recherché (référence, désignation, spécifications)",
              example = "pelle")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Soumis à partir du (inclus)", example = "2026-01-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(description = "Soumis jusqu'au (inclus)", example = "2026-12-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au,
      @Parameter(description = "Page (base 0)", example = "0") @RequestParam(required = false)
          Integer page,
      @Parameter(description = "Taille (max 50)", example = "20") @RequestParam(required = false)
          Integer size,
      @Parameter(description = "Tri champ,asc|desc", example = "createdAt,desc")
          @RequestParam(required = false)
          String sort) {
    PageQuery requetePage = pages.query(page, size, sort, TRIABLES, TRI_PAR_DEFAUT);
    CriteresRechercheBesoins criteres =
        new CriteresRechercheBesoins(chantierId, statut, priorite, categorie, q, du, au, null);
    return consultation
        .rechercher(criteres, requetePage, utilisateur())
        .map(BesoinResponse::depuis);
  }

  /**
   * @param id besoin
   * @param requete commentaire et matériels souhaités (facultatif)
   * @return le besoin validé et orienté
   */
  @PatchMapping("/{id}/valider")
  @PreAuthorize(RM)
  @Operation(
      summary = "Valider un besoin (Responsable Matériel, R01)",
      description =
          "Valide un besoin EN_ATTENTE puis l'oriente : interrogation de la disponibilité du parc"
              + " (ms-suivi) ; si elle suffit, affectation des matériels (ceux de materielIds en"
              + " priorité) du dateBesoin au dateBesoin + dureeUtilisationJours → AFFECTE_DIRECT ;"
              + " sinon → EN_ACHAT. Si ms-suivi est indisponible, le besoin reste VALIDE avec"
              + " orientationEnAttente=true. Publie BESOIN_VALIDE puis BESOIN_AFFECTE_DIRECT ou"
              + " BESOIN_EN_ACHAT.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Besoin validé (et orienté si possible)"),
    @ApiResponse(
        responseCode = "400",
        description = "Champs invalides",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Réservé au Responsable Matériel (R01, VALIDATION_NON_AUTORISEE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Besoin déjà traité (TRANSITION_STATUT_INTERDITE) ou CONFLIT_CONCURRENCE",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public BesoinResponse valider(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id,
      @Valid @RequestBody(required = false) ValidationBesoinRequest requete) {
    ValidationBesoinRequest corps =
        requete == null ? new ValidationBesoinRequest(null, List.of()) : requete;
    return BesoinResponse.depuis(
        validation.valider(
            new ValiderBesoinCommand(id, corps.commentaire(), corps.materielIds(), utilisateur())));
  }

  /**
   * @param id besoin
   * @param requete matériels souhaités (facultatif)
   * @return le besoin orienté
   */
  @PostMapping("/{id}/orienter")
  @PreAuthorize(RM)
  @Operation(
      summary = "Relancer l'orientation d'un besoin validé",
      description =
          "Pour un besoin VALIDE dont orientationEnAttente=true (ms-suivi était indisponible) :"
              + " même logique que la validation. Déclenché par un Responsable Matériel car"
              + " l'affectation exige son JWT (pas de tâche système).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Besoin orienté (ou toujours en attente)"),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Réservé au Responsable Matériel",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Aucune orientation en attente (ORIENTATION_NON_REQUISE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public BesoinResponse orienter(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id,
      @Valid @RequestBody(required = false) OrientationBesoinRequest requete) {
    List<UUID> materiels = requete == null ? List.of() : requete.materielIds();
    return BesoinResponse.depuis(
        orientation.orienter(new OrienterBesoinCommand(id, materiels, utilisateur())));
  }

  /**
   * @param id besoin
   * @param requete motif
   * @return le besoin rejeté
   */
  @PatchMapping("/{id}/rejeter")
  @PreAuthorize(RM)
  @Operation(
      summary = "Rejeter un besoin (Responsable Matériel, R01)",
      description = "Rejette un besoin EN_ATTENTE ; motif obligatoire (R08). Publie BESOIN_REJETE.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Besoin rejeté"),
    @ApiResponse(
        responseCode = "400",
        description = "Champs invalides",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Réservé au Responsable Matériel (R01)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Besoin déjà traité (TRANSITION_STATUT_INTERDITE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Motif absent (MOTIF_REJET_OBLIGATOIRE, R08)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public BesoinResponse rejeter(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id,
      @Valid @RequestBody RejetBesoinRequest requete) {
    return BesoinResponse.depuis(
        rejet.rejeter(new RejeterBesoinCommand(id, requete.motif(), utilisateur())));
  }

  /**
   * @param id besoin
   * @return l'historique chronologique
   */
  @GetMapping("/{id}/historique")
  @PreAuthorize(LECTEURS)
  @Operation(
      summary = "Historique d'un besoin",
      description = "Toutes les actions sur le besoin, dans l'ordre chronologique.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Historique",
        content =
            @Content(
                array =
                    @ArraySchema(
                        schema = @Schema(implementation = HistoriqueBesoinResponse.class)))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou chantier non attribué",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Besoin introuvable",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public List<HistoriqueBesoinResponse> historique(
      @Parameter(description = "Identifiant du besoin") @PathVariable UUID id) {
    return consultation.historique(id, utilisateur()).stream()
        .map(HistoriqueBesoinResponse::depuis)
        .toList();
  }

  /**
   * @param statut EN_ATTENTE ou VALIDE (les deux par défaut)
   * @return les regroupements
   */
  @GetMapping("/consolidation")
  @PreAuthorize(RM)
  @Operation(
      summary = "Consolider les besoins",
      description =
          "Regroupe les besoins EN_ATTENTE et/ou VALIDE par catégorie et désignation normalisée"
              + " (casse, accents, ponctuation ignorés) et signale les doublons potentiels : même"
              + " chantier, même désignation, dates de besoin proches (fenêtre configurable).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Regroupements",
        content =
            @Content(
                array =
                    @ArraySchema(schema = @Schema(implementation = ConsolidationResponse.class)))),
    @ApiResponse(
        responseCode = "400",
        description = "Statut non consolidable (STATUT_NON_CONSOLIDABLE)",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Réservé au Responsable Matériel",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public List<ConsolidationResponse> consolidation(
      @Parameter(description = "EN_ATTENTE ou VALIDE (les deux si absent)", example = "EN_ATTENTE")
          @RequestParam(required = false)
          StatutBesoin statut) {
    return consolidation.consolider(statut).stream().map(ConsolidationResponse::depuis).toList();
  }

  /**
   * @return les statistiques
   */
  @GetMapping("/statistiques")
  @PreAuthorize(INTERNES)
  @Operation(
      summary = "Statistiques des besoins",
      description =
          "Répartition par statut et priorité, délai moyen de décision. Restreint à ses chantiers"
              + " pour un chef de chantier.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Statistiques"),
    @ApiResponse(
        responseCode = "401",
        description = "Non authentifié",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant",
        content =
            @Content(mediaType = PROBLEME, schema = @Schema(implementation = ProblemDetail.class)))
  })
  public StatistiquesBesoinsResponse statistiques() {
    return StatistiquesBesoinsResponse.depuis(statistiques.statistiques(utilisateur()));
  }

  private CurrentUser utilisateur() {
    return utilisateurs.require();
  }
}

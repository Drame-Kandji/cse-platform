package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.api.dto.CarteDto;
import sn.cse.suivi.api.dto.DisponibiliteDto;
import sn.cse.suivi.api.dto.MaterielDetailDto;
import sn.cse.suivi.api.dto.MaterielDto;
import sn.cse.suivi.api.dto.MaterielRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.port.in.ConsulterCarteUseCase;
import sn.cse.suivi.application.port.in.ConsulterMaterielsUseCase;
import sn.cse.suivi.application.port.in.GererMaterielsUseCase;
import sn.cse.suivi.application.port.in.VerifierDisponibiliteUseCase;
import sn.cse.suivi.application.query.CritereDisponibilite;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/** Parc matériel : consultation, fiche détaillée, disponibilité, carte, saisie des fiches. */
@RestController
@RequestMapping("/api/materiels")
@Tag(name = "Matériels", description = "Parc matériel de la CSE")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant ou chantier hors périmètre")
public class MaterielController {

  /** Champs triables. */
  static final Set<String> TRIABLES =
      Set.of(
          "code",
          "designation",
          "categorie",
          "etat",
          "heuresFonctionnement",
          "tarifJournalier",
          "prochaineMaintenance");

  private static final String TRI_DEFAUT = "code,asc";

  private final ConsulterMaterielsUseCase consultation;
  private final GererMaterielsUseCase gestion;
  private final VerifierDisponibiliteUseCase disponibilite;
  private final ConsulterCarteUseCase carte;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param consultation consultation du parc
   * @param gestion saisie des fiches
   * @param disponibilite vérification de disponibilité
   * @param carte carte du parc
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   */
  public MaterielController(
      ConsulterMaterielsUseCase consultation,
      GererMaterielsUseCase gestion,
      VerifierDisponibiliteUseCase disponibilite,
      ConsulterCarteUseCase carte,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.consultation = consultation;
    this.gestion = gestion;
    this.disponibilite = disponibilite;
    this.carte = carte;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param etat état
   * @param categorie catégorie
   * @param chantierId chantier courant
   * @param q texte libre
   * @param maintenanceDue échéance de maintenance atteinte
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page de matériels
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Lister les matériels",
      description = "Un chef de chantier ne voit que les matériels présents sur ses chantiers.")
  @ApiResponse(responseCode = "200", description = "Page de matériels")
  public PageResponse<MaterielDto> lister(
      @Parameter(example = "DISPONIBLE") @RequestParam(required = false) EtatMateriel etat,
      @Parameter(example = "MATERIEL_ROULANT") @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(example = "c0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID chantierId,
      @Parameter(description = "Code, désignation, immatriculation ou marque", example = "pelle")
          @RequestParam(required = false)
          String q,
      @Parameter(example = "true") @RequestParam(required = false) Boolean maintenanceDue,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(
              description =
                  "Tri : code, designation, categorie, etat, heuresFonctionnement,"
                      + " tarifJournalier, prochaineMaintenance",
              example = "designation,asc")
          @RequestParam(required = false)
          String sort) {
    return consultation
        .rechercher(
            new ConsulterMaterielsUseCase.Criteres(etat, categorie, chantierId, q, maintenanceDue),
            pages.query(page, size, sort, TRIABLES, TRI_DEFAUT),
            utilisateurs.require())
        .map(ApiMapper::materiel);
  }

  /**
   * @param id matériel
   * @return la fiche détaillée
   */
  @GetMapping("/{id}")
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Consulter la fiche d'un matériel",
      description = "Champs du matériel et 20 derniers éléments de chaque historique.")
  @ApiResponse(responseCode = "200", description = "Fiche du matériel")
  @ApiResponse(responseCode = "404", description = "Matériel introuvable")
  public MaterielDetailDto detail(
      @Parameter(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id) {
    return ApiMapper.detail(consultation.detail(id, utilisateurs.require()));
  }

  /**
   * @param requete caractéristiques
   * @return le matériel enregistré
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Enregistrer un matériel",
      description = "Entrée dans le parc à l'état DISPONIBLE ; émet MATERIEL_ENREGISTRE.")
  @ApiResponse(responseCode = "201", description = "Matériel enregistré")
  @ApiResponse(responseCode = "404", description = "Type d'engin introuvable")
  @ApiResponse(responseCode = "409", description = "Code déjà utilisé")
  @ApiResponse(responseCode = "422", description = "Type d'engin inactif ou incompatible")
  public MaterielDto enregistrer(@Valid @RequestBody MaterielRequest requete) {
    return ApiMapper.materiel(
        gestion.enregistrer(
            ApiMapper.description(requete),
            requete.heuresFonctionnement(),
            utilisateurs.require()));
  }

  /**
   * @param id matériel
   * @param requete caractéristiques et version
   * @return le matériel modifié
   */
  @PutMapping("/{id}")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Modifier un matériel",
      description =
          "Champs descriptifs uniquement (le compteur se met à jour par relevé) ; version"
              + " obligatoire.")
  @ApiResponse(responseCode = "200", description = "Matériel modifié")
  @ApiResponse(responseCode = "404", description = "Matériel ou type d'engin introuvable")
  @ApiResponse(responseCode = "409", description = "Version obsolète ou code déjà utilisé")
  @ApiResponse(responseCode = "422", description = "Matériel réformé ou type incompatible")
  public MaterielDto modifier(@PathVariable UUID id, @Valid @RequestBody MaterielRequest requete) {
    return ApiMapper.materiel(
        gestion.modifier(
            id,
            ApiMapper.description(requete),
            ChantierController.versionObligatoire(requete.version()),
            utilisateurs.require()));
  }

  /**
   * @param categorie catégorie
   * @param designation fragment de désignation
   * @param quantite quantité souhaitée
   * @param du début de période
   * @param au fin de période
   * @return la disponibilité
   */
  @GetMapping("/disponibilite")
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Vérifier la disponibilité",
      description =
          "Matériels DISPONIBLE sans demande de réforme ni maintenance planifiée sur la période"
              + " (utilisé par ms-besoins pour orienter un besoin).")
  @ApiResponse(responseCode = "200", description = "Disponibilité")
  @ApiResponse(responseCode = "422", description = "Période incohérente")
  public DisponibiliteDto disponibilite(
      @Parameter(example = "MATERIEL_ROULANT") @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(example = "Pelle") @RequestParam(required = false) String designation,
      @Parameter(example = "2") @RequestParam(required = false) Integer quantite,
      @Parameter(example = "2026-11-02")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(example = "2027-02-28")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au) {
    return ApiMapper.disponibilite(
        disponibilite.verifier(new CritereDisponibilite(categorie, designation, quantite, du, au)));
  }

  /**
   * @param categorie catégorie
   * @param etat état
   * @return la FeatureCollection GeoJSON
   */
  @GetMapping("/carte")
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Carte du parc (GeoJSON)",
      description =
          "FeatureCollection des chantiers (kind=CHANTIER) et des matériels géolocalisés"
              + " (kind=MATERIEL) ; les matériels réformés sont exclus sauf filtre etat=REFORME.")
  @ApiResponse(responseCode = "200", description = "FeatureCollection GeoJSON")
  public CarteDto carte(
      @Parameter(example = "MATERIEL_ROULANT") @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(example = "AFFECTE") @RequestParam(required = false) EtatMateriel etat) {
    return ApiMapper.carte(carte.carte(categorie, etat, utilisateurs.require()));
  }
}

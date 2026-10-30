package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.fournisseurs.api.dto.ClassementDto;
import sn.cse.fournisseurs.api.dto.DecisionBlacklistageRequest;
import sn.cse.fournisseurs.api.dto.DocumentDto;
import sn.cse.fournisseurs.api.dto.DocumentRequest;
import sn.cse.fournisseurs.api.dto.FournisseurDetailDto;
import sn.cse.fournisseurs.api.dto.FournisseurDto;
import sn.cse.fournisseurs.api.dto.FournisseurRequest;
import sn.cse.fournisseurs.api.dto.ModifierFournisseurRequest;
import sn.cse.fournisseurs.api.dto.MotifRequest;
import sn.cse.fournisseurs.api.dto.StatistiquesFournisseursDto;
import sn.cse.fournisseurs.api.mapper.DtoMapper;
import sn.cse.fournisseurs.application.command.DocumentCommand;
import sn.cse.fournisseurs.application.port.in.ConsulterFournisseursUseCase;
import sn.cse.fournisseurs.application.port.in.GererCycleVieUseCase;
import sn.cse.fournisseurs.application.port.in.GererReferentielUseCase;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/** Référentiel fournisseurs : consultation, création, cycle de vie, documents (contrat §4). */
@RestController
@RequestMapping("/api/fournisseurs")
@Tag(
    name = "Fournisseurs",
    description = "Référentiel, cycle de vie et blacklistage (R09) des fournisseurs")
public class FournisseurController {

  /** Champs de tri autorisés. */
  static final Set<String> TRIABLES =
      Set.of("raisonSociale", "scorePerformance", "dateInscription", "statut", "ville");

  private static final String TRI_DEFAUT = "raisonSociale,asc";

  private final ConsulterFournisseursUseCase consultation;
  private final GererReferentielUseCase referentiel;
  private final GererCycleVieUseCase cycleVie;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final Clock clock;

  /**
   * @param consultation lecture
   * @param referentiel gestion du référentiel
   * @param cycleVie transitions de statut
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   * @param clock horloge (expiration des documents)
   */
  public FournisseurController(
      ConsulterFournisseursUseCase consultation,
      GererReferentielUseCase referentiel,
      GererCycleVieUseCase cycleVie,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      Clock clock) {
    this.clock = clock;
    this.consultation = consultation;
    this.referentiel = referentiel;
    this.cycleVie = cycleVie;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param q texte libre
   * @param specialite spécialité
   * @param statut statut
   * @param page page
   * @param size taille
   * @param sort tri
   * @return page de fournisseurs
   */
  @GetMapping
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Rechercher des fournisseurs",
      description =
          "Liste paginée filtrable par texte, spécialité et statut. R14 : ms-achats l'appelle avec"
              + " statut=ACTIF&specialite=X pour vérifier les fournisseurs invités. Tri autorisé :"
              + " raisonSociale, scorePerformance, dateInscription, statut, ville.")
  @ApiResponse(responseCode = "200", description = "Page de fournisseurs")
  public PageResponse<FournisseurDto> rechercher(
      @Parameter(
              description = "Texte libre (raison sociale, NINEA, ville, e-mail)",
              example = "Sahel")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Spécialité", example = "MATERIEL_ROULANT")
          @RequestParam(required = false)
          CategorieMateriel specialite,
      @Parameter(description = "Statut", example = "ACTIF") @RequestParam(required = false)
          StatutFournisseur statut,
      @Parameter(description = "Numéro de page (0..)", example = "0")
          @RequestParam(required = false)
          Integer page,
      @Parameter(description = "Taille de page (max 50)", example = "20")
          @RequestParam(required = false)
          Integer size,
      @Parameter(description = "Tri champ,asc|desc", example = "scorePerformance,desc")
          @RequestParam(required = false)
          String sort) {
    PageQuery requete = pages.query(page, size, sort, TRIABLES, TRI_DEFAUT);
    return consultation
        .rechercher(new CriteresFournisseurs(q, specialite, statut), requete)
        .map(DtoMapper::fournisseur);
  }

  /**
   * @param id fournisseur
   * @return détail
   */
  @GetMapping("/{id}")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Consulter un fournisseur",
      description =
          "Détail avec documents administratifs. Les coordonnées bancaires ne sont renvoyées qu'au"
              + " Responsable des Achats et à la DAF (null pour les autres rôles).")
  @ApiResponse(responseCode = "200", description = "Fournisseur détaillé")
  public FournisseurDetailDto detail(
      @Parameter(description = "Identifiant", example = "f0000000-0000-0000-0000-000000000001")
          @PathVariable
          UUID id) {
    return DtoMapper.detail(consultation.detail(id, utilisateurs.require()));
  }

  /**
   * @param requete données
   * @return fournisseur créé
   */
  @PostMapping
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Référencer un fournisseur",
      description =
          "Création directe par le Responsable des Achats : statut ACTIF, événement FOURNISSEUR_INSCRIT.")
  @ApiResponse(responseCode = "201", description = "Fournisseur créé")
  public ResponseEntity<FournisseurDetailDto> creer(
      @Valid @RequestBody FournisseurRequest requete) {
    FournisseurVue vue = referentiel.creer(DtoMapper.commande(requete), utilisateurs.require());
    return ResponseEntity.created(URI.create("/api/fournisseurs/" + vue.fournisseur().getId()))
        .body(DtoMapper.detail(vue));
  }

  /**
   * @param id fournisseur
   * @param requete données + version
   * @return fournisseur modifié
   */
  @PutMapping("/{id}")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Modifier un fournisseur",
      description =
          "Mise à jour du référentiel (verrou optimiste : version obsolète → 409 CONFLIT_CONCURRENCE)."
              + " Coordonnées bancaires absentes = inchangées. Refusé pour un fournisseur blacklisté.")
  @ApiResponse(responseCode = "200", description = "Fournisseur modifié")
  public FournisseurDetailDto modifier(
      @PathVariable UUID id, @Valid @RequestBody ModifierFournisseurRequest requete) {
    return DtoMapper.detail(
        referentiel.modifier(
            id, requete.version(), DtoMapper.commande(requete), utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @return fournisseur
   */
  @PatchMapping("/{id}/valider-inscription")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Valider une inscription",
      description =
          "EN_ATTENTE_VALIDATION → ACTIF (toute autre origine → 409 TRANSITION_STATUT_INVALIDE).")
  @ApiResponse(responseCode = "200", description = "Fournisseur actif")
  public FournisseurDto validerInscription(@PathVariable UUID id) {
    return DtoMapper.fournisseur(cycleVie.validerInscription(id, utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @param requete motif
   * @return fournisseur
   */
  @PatchMapping("/{id}/suspendre")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Suspendre un fournisseur",
      description = "ACTIF → SUSPENDU, motif obligatoire.")
  @ApiResponse(responseCode = "200", description = "Fournisseur suspendu")
  public FournisseurDto suspendre(@PathVariable UUID id, @Valid @RequestBody MotifRequest requete) {
    return DtoMapper.fournisseur(cycleVie.suspendre(id, requete.motif(), utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @return fournisseur
   */
  @PatchMapping("/{id}/reactiver")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Réactiver un fournisseur",
      description = "SUSPENDU → ACTIF (refusé si une demande de blacklistage est en attente).")
  @ApiResponse(responseCode = "200", description = "Fournisseur réactivé")
  public FournisseurDto reactiver(@PathVariable UUID id) {
    return DtoMapper.fournisseur(cycleVie.reactiver(id, utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @param requete motif
   * @return fournisseur
   */
  @PostMapping("/{id}/blacklistage/demande")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Demander le blacklistage (R09, étape 1)",
      description =
          "Le Responsable des Achats demande le blacklistage d'un fournisseur SUSPENDU ; le statut"
              + " ne change qu'après décision de la Direction Générale.")
  @ApiResponse(responseCode = "200", description = "Demande enregistrée")
  public FournisseurDto demanderBlacklistage(
      @PathVariable UUID id, @Valid @RequestBody MotifRequest requete) {
    return DtoMapper.fournisseur(
        cycleVie.demanderBlacklistage(id, requete.motif(), utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @param requete décision
   * @return fournisseur
   */
  @PatchMapping("/{id}/blacklister")
  @PreAuthorize(Acces.DG_SEULE)
  @Operation(
      summary = "Statuer sur le blacklistage (R09, étape 2)",
      description =
          "La Direction Générale approuve (SUSPENDU → BLACKLISTE) ou refuse la demande en attente.")
  @ApiResponse(responseCode = "200", description = "Décision appliquée")
  public FournisseurDto blacklister(
      @PathVariable UUID id, @Valid @RequestBody DecisionBlacklistageRequest requete) {
    return DtoMapper.fournisseur(
        cycleVie.deciderBlacklistage(
            id, requete.approuve(), requete.commentaire(), utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @param requete document
   * @return document créé
   */
  @PostMapping("/{id}/documents")
  @PreAuthorize(Acces.RA_SEUL)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Ajouter un document administratif",
      description = "Le job quotidien émet DOCUMENT_EXPIRE à l'expiration.")
  @ApiResponse(responseCode = "201", description = "Document enregistré")
  public DocumentDto ajouterDocument(
      @PathVariable UUID id, @Valid @RequestBody DocumentRequest requete) {
    return DtoMapper.document(
        referentiel.ajouterDocument(
            id, new DocumentCommand(requete.type(), requete.libelle(), requete.dateExpiration())),
        LocalDate.now(clock));
  }

  /**
   * @param categorie catégorie
   * @return classement
   */
  @GetMapping("/classement")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Classement des fournisseurs",
      description =
          "Classement dynamique des fournisseurs ACTIF par score décroissant, puis ponctualité et"
              + " nombre d'évaluations.")
  @ApiResponse(responseCode = "200", description = "Classement")
  public List<ClassementDto> classement(
      @Parameter(description = "Catégorie", example = "MATERIEL_ROULANT")
          @RequestParam(required = false)
          CategorieMateriel categorie) {
    return consultation.classement(categorie).stream().map(DtoMapper::classement).toList();
  }

  /**
   * @return statistiques
   */
  @GetMapping("/statistiques")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Statistiques du référentiel",
      description = "Compteurs par statut, score moyen, litiges et documents expirés.")
  @ApiResponse(responseCode = "200", description = "Statistiques")
  public StatistiquesFournisseursDto statistiques() {
    return DtoMapper.statistiques(consultation.statistiques());
  }
}

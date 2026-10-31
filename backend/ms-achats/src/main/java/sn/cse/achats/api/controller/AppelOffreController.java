package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import sn.cse.achats.api.dto.AppelOffreDto;
import sn.cse.achats.api.dto.AttribuerRequest;
import sn.cse.achats.api.dto.ComparatifDto;
import sn.cse.achats.api.dto.CreerAppelOffreRequest;
import sn.cse.achats.api.dto.DecisionRequest;
import sn.cse.achats.api.dto.EvaluationRequest;
import sn.cse.achats.api.dto.OffreDto;
import sn.cse.achats.api.dto.SaisirOffreRequest;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.command.CreerAppelOffreCommand;
import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.command.DeposerOffreCommand;
import sn.cse.achats.application.command.EvaluerOffreCommand;
import sn.cse.achats.application.command.ProposerAttributionCommand;
import sn.cse.achats.application.port.in.AttribuerMarcheUseCase;
import sn.cse.achats.application.port.in.CloturerAppelOffreUseCase;
import sn.cse.achats.application.port.in.ConsulterAppelsOffresUseCase;
import sn.cse.achats.application.port.in.DeposerOffreUseCase;
import sn.cse.achats.application.port.in.DocumentPdf;
import sn.cse.achats.application.port.in.EvaluerOffresUseCase;
import sn.cse.achats.application.port.in.GenererProcesVerbalUseCase;
import sn.cse.achats.application.port.in.PublierAppelOffreUseCase;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/** Appels d'offres : publication, offres, clôture, évaluation, attribution et procès-verbaux. */
@RestController
@RequestMapping("/api/appels-offres")
@Tag(name = "Appels d'offres", description = "Cycle de mise en concurrence (R02, R10, R11, R14)")
public class AppelOffreController {

  private final PublierAppelOffreUseCase publication;
  private final DeposerOffreUseCase depot;
  private final CloturerAppelOffreUseCase cloture;
  private final EvaluerOffresUseCase evaluation;
  private final AttribuerMarcheUseCase attribution;
  private final ConsulterAppelsOffresUseCase consultation;
  private final GenererProcesVerbalUseCase procesVerbaux;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final AchatsApiMapper mapper;

  /**
   * @param publication publication
   * @param depot dépôt d'offres
   * @param cloture clôture
   * @param evaluation évaluation
   * @param attribution attribution
   * @param consultation lectures
   * @param procesVerbaux procès-verbaux
   * @param pages pagination
   * @param utilisateurs utilisateur courant
   * @param mapper conversion DTO
   */
  public AppelOffreController(
      PublierAppelOffreUseCase publication,
      DeposerOffreUseCase depot,
      CloturerAppelOffreUseCase cloture,
      EvaluerOffresUseCase evaluation,
      AttribuerMarcheUseCase attribution,
      ConsulterAppelsOffresUseCase consultation,
      GenererProcesVerbalUseCase procesVerbaux,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      AchatsApiMapper mapper) {
    this.publication = publication;
    this.depot = depot;
    this.cloture = cloture;
    this.evaluation = evaluation;
    this.attribution = attribution;
    this.consultation = consultation;
    this.procesVerbaux = procesVerbaux;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
    this.mapper = mapper;
  }

  /**
   * @param statut statut
   * @param type procédure
   * @param q recherche
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page
   */
  @GetMapping
  @PreAuthorize(Acces.LECTURE_AO)
  @Operation(
      summary = "Lister les appels d'offres",
      description =
          "Filtres facultatifs : statut, procédure, texte (référence ou objet). Tri autorisé :"
              + " datePublication, dateLimite, reference, objet, statut (défaut datePublication,desc).")
  @ApiResponse(responseCode = "200", description = "Page d'appels d'offres")
  public PageResponse<AppelOffreDto> lister(
      @Parameter(example = "PUBLIE") @RequestParam(required = false) StatutAO statut,
      @Parameter(example = "APPEL_OFFRES") @RequestParam(required = false) TypeAppelOffre type,
      @Parameter(example = "pelle") @RequestParam(required = false) String q,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "datePublication,desc") @RequestParam(required = false) String sort) {
    return consultation
        .rechercher(
            new AppelOffreRepository.Filtre(statut, type, q),
            pages.query(page, size, sort, AppelOffreRepository.TRIABLES, "datePublication,desc"))
        .map(mapper::appelOffre);
  }

  /**
   * @param id appel d'offres
   * @return le détail
   */
  @GetMapping("/{id}")
  @PreAuthorize(Acces.LECTURE_AO)
  @Operation(summary = "Consulter un appel d'offres")
  @ApiResponse(responseCode = "200", description = "Appel d'offres")
  public AppelOffreDto detail(@PathVariable UUID id) {
    return mapper.appelOffre(consultation.detail(id));
  }

  /**
   * @param requete données
   * @return l'appel d'offres publié
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Publier un appel d'offres",
      description =
          "Statut PUBLIE immédiat. Chaque invité est vérifié auprès de ms-fournisseurs (repli sur la"
              + " projection locale) et doit être ACTIF (R14, 422 FOURNISSEUR_NON_ELIGIBLE). Somme"
              + " des poids = 100 (422 PONDERATION_INVALIDE).")
  @ApiResponse(responseCode = "201", description = "Appel d'offres publié")
  public AppelOffreDto publier(@Valid @RequestBody CreerAppelOffreRequest requete) {
    List<CriterePondere> criteres =
        requete.criteres().stream().map(c -> new CriterePondere(c.code(), c.poids())).toList();
    return mapper.appelOffre(
        publication.publier(
            new CreerAppelOffreCommand(
                requete.besoinId(),
                requete.objet(),
                requete.description(),
                requete.type(),
                requete.categorie(),
                requete.quantite(),
                requete.specifications(),
                requete.budgetEstime(),
                requete.dateLimite(),
                criteres,
                requete.fournisseurIdsInvites(),
                utilisateurs.require())));
  }

  /**
   * @param id appel d'offres
   * @param requete offre reçue
   * @return l'offre (anonymisée)
   */
  @PostMapping("/{id}/offres")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Saisir une offre reçue hors portail",
      description =
          "Refusée après la date limite (R02, 422 DATE_LIMITE_DEPASSEE) ou pour un fournisseur"
              + " non actif (R14, 422 FOURNISSEUR_NON_ELIGIBLE). Une offre par fournisseur (409"
              + " OFFRE_DEJA_DEPOSEE).")
  @ApiResponse(responseCode = "201", description = "Offre enregistrée (anonymisée, R10)")
  public OffreDto saisirOffre(
      @PathVariable UUID id, @Valid @RequestBody SaisirOffreRequest requete) {
    return mapper.offre(
        depot.saisirOffre(
            new DeposerOffreCommand(
                id,
                requete.fournisseurId(),
                requete.montantPropose(),
                requete.delaiLivraisonJours(),
                requete.garantieMois(),
                requete.description(),
                utilisateurs.require())));
  }

  /**
   * @param id appel d'offres
   * @return les offres
   */
  @GetMapping("/{id}/offres")
  @PreAuthorize(Acces.LECTURE_OFFRES)
  @Operation(
      summary = "Lister les offres",
      description =
          "Tant que l'appel d'offres est PUBLIE, fournisseurId et fournisseurNom sont null et"
              + " aucune note n'est calculée (R10).")
  @ApiResponse(
      responseCode = "200",
      description = "Offres",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = OffreDto.class))))
  public List<OffreDto> offres(@PathVariable UUID id) {
    return consultation.offres(id).stream().map(mapper::offre).toList();
  }

  /**
   * @param id appel d'offres
   * @return l'appel d'offres clôturé
   */
  @PostMapping("/{id}/cloturer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Clôturer un appel d'offres",
      description =
          "PUBLIE → CLOTURE ; l'anonymat des offres est levé. Effectué automatiquement à la date"
              + " limite par un traitement planifié.")
  @ApiResponse(responseCode = "200", description = "Appel d'offres clôturé")
  public AppelOffreDto cloturer(@PathVariable UUID id) {
    return mapper.appelOffre(cloture.cloturer(id));
  }

  /**
   * @param id appel d'offres
   * @param offreId offre
   * @param requete notes
   * @return l'offre réévaluée
   */
  @PutMapping("/{id}/offres/{offreId}/evaluation")
  @PreAuthorize(Acces.ACHATS_MATERIEL)
  @Operation(
      summary = "Noter une offre",
      description =
          "Notes 0..100 des critères saisis (QUALITE_TECHNIQUE, GARANTIE, SAV, REFERENCES) ; PRIX"
              + " et DELAI sont calculés par rapport à la meilleure offre. Appel d'offres CLOTURE"
              + " requis (409 AO_NON_CLOTURE).")
  @ApiResponse(responseCode = "200", description = "Offre réévaluée")
  public OffreDto evaluer(
      @PathVariable UUID id,
      @PathVariable UUID offreId,
      @Valid @RequestBody EvaluationRequest requete) {
    Map<CritereEvaluation, Integer> notes = new EnumMap<>(CritereEvaluation.class);
    ajouter(notes, CritereEvaluation.QUALITE_TECHNIQUE, requete.qualiteTechnique());
    ajouter(notes, CritereEvaluation.GARANTIE, requete.garantie());
    ajouter(notes, CritereEvaluation.SAV, requete.sav());
    ajouter(notes, CritereEvaluation.REFERENCES, requete.references());
    return mapper.offre(evaluation.evaluer(new EvaluerOffreCommand(id, offreId, notes)));
  }

  /**
   * @param id appel d'offres
   * @return le comparatif
   */
  @GetMapping("/{id}/comparatif")
  @PreAuthorize(Acces.LECTURE_OFFRES)
  @Operation(
      summary = "Comparatif des offres",
      description =
          "Score global pondéré, rang et recommandation (offre classée première). Disponible après"
              + " la clôture (409 AO_NON_CLOTURE).")
  @ApiResponse(responseCode = "200", description = "Comparatif")
  public ComparatifDto comparatif(@PathVariable UUID id) {
    return mapper.comparatif(evaluation.comparatif(id));
  }

  /**
   * @param id appel d'offres
   * @param requete proposition
   * @return l'appel d'offres et sa proposition
   */
  @PostMapping("/{id}/attribuer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Proposer l'attribution",
      description =
          "Crée la proposition d'attribution (R11) : co-validation RM toujours requise,"
              + " approbation DG requise si le montant dépasse cse.achats.seuil-delegation.")
  @ApiResponse(responseCode = "200", description = "Proposition créée")
  public AppelOffreDto attribuer(
      @PathVariable UUID id, @Valid @RequestBody AttribuerRequest requete) {
    return mapper.appelOffre(
        attribution.proposer(
            new ProposerAttributionCommand(
                id, requete.offreId(), requete.justification(), utilisateurs.require())));
  }

  /**
   * @param id appel d'offres
   * @param requete décision
   * @return l'appel d'offres
   */
  @PostMapping("/{id}/attribution/co-valider")
  @PreAuthorize(Acces.MATERIEL)
  @Operation(
      summary = "Co-valider l'attribution (Responsable Matériel)",
      description =
          "R11. Refus → proposition supprimée. Si toutes les approbations sont acquises : AO"
              + " ATTRIBUE et contrat BROUILLON généré (R03).")
  @ApiResponse(responseCode = "200", description = "Décision enregistrée")
  public AppelOffreDto coValider(
      @PathVariable UUID id, @Valid @RequestBody DecisionRequest requete) {
    return mapper.appelOffre(
        attribution.coValider(
            new DecisionCommand(
                id, requete.approuve(), requete.commentaire(), utilisateurs.require())));
  }

  /**
   * @param id appel d'offres
   * @param requete décision
   * @return l'appel d'offres
   */
  @PostMapping("/{id}/attribution/approuver")
  @PreAuthorize(Acces.DG)
  @Operation(
      summary = "Approuver l'attribution (Direction Générale)",
      description =
          "R11 : requise uniquement au-delà du seuil de délégation (409"
              + " APPROBATION_DG_NON_REQUISE sinon).")
  @ApiResponse(responseCode = "200", description = "Décision enregistrée")
  public AppelOffreDto approuver(
      @PathVariable UUID id, @Valid @RequestBody DecisionRequest requete) {
    return mapper.appelOffre(
        attribution.approuver(
            new DecisionCommand(
                id, requete.approuve(), requete.commentaire(), utilisateurs.require())));
  }

  /**
   * @param id appel d'offres
   * @param type type de procès-verbal
   * @return le PDF
   */
  @GetMapping(value = "/{id}/proces-verbaux/{type}", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize(Acces.LECTURE_OFFRES)
  @Operation(
      summary = "Télécharger un procès-verbal (PDF)",
      description =
          "OUVERTURE disponible après la clôture, EVALUATION après l'attribution (409"
              + " PV_INDISPONIBLE sinon).")
  @ApiResponse(
      responseCode = "200",
      description = "Document PDF",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PDF_VALUE,
              schema = @Schema(type = "string", format = "binary")))
  public ResponseEntity<byte[]> procesVerbal(
      @PathVariable UUID id,
      @Parameter(example = "OUVERTURE") @PathVariable TypeProcesVerbal type) {
    DocumentPdf pdf = procesVerbaux.generer(id, type);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.nomFichier() + "\"")
        .body(pdf.contenu());
  }

  private static void ajouter(
      Map<CritereEvaluation, Integer> notes, CritereEvaluation critere, Integer note) {
    if (note != null) {
      notes.put(critere, note);
    }
  }
}

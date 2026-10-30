package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.suivi.api.dto.AffectationDto;
import sn.cse.suivi.api.dto.AffecterRequest;
import sn.cse.suivi.api.dto.ChangementEtatRequest;
import sn.cse.suivi.api.dto.ConfirmerReceptionRequest;
import sn.cse.suivi.api.dto.DecisionReformeRequest;
import sn.cse.suivi.api.dto.DemandeReformeRequest;
import sn.cse.suivi.api.dto.LibererRequest;
import sn.cse.suivi.api.dto.MaterielDto;
import sn.cse.suivi.api.dto.ReleveHeuresRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.command.AffecterMaterielCommande;
import sn.cse.suivi.application.command.ConfirmerReceptionCommande;
import sn.cse.suivi.application.command.LibererMaterielCommande;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.in.ChangerEtatMaterielUseCase;
import sn.cse.suivi.application.port.in.ConfirmerReceptionUseCase;
import sn.cse.suivi.application.port.in.LibererMaterielUseCase;
import sn.cse.suivi.application.port.in.ReformerMaterielUseCase;
import sn.cse.suivi.application.port.in.ReleverHeuresUseCase;

/** Opérations sur un matériel : affectation, réception, libération, relevé, état, réforme. */
@RestController
@RequestMapping("/api/materiels/{id}")
@Tag(name = "Matériels — opérations", description = "Cycle de vie d'un matériel")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant ou chantier hors périmètre")
@ApiResponse(responseCode = "404", description = "Matériel (ou chantier, affectation) introuvable")
public class MaterielOperationsController {

  private final AffecterMaterielUseCase affectation;
  private final ConfirmerReceptionUseCase reception;
  private final LibererMaterielUseCase liberation;
  private final ReleverHeuresUseCase releve;
  private final ChangerEtatMaterielUseCase changementEtat;
  private final ReformerMaterielUseCase reforme;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param affectation affectation
   * @param reception confirmation de réception
   * @param liberation libération
   * @param releve relevé d'heures
   * @param changementEtat changement d'état manuel
   * @param reforme réforme
   * @param utilisateurs utilisateur courant
   */
  public MaterielOperationsController(
      AffecterMaterielUseCase affectation,
      ConfirmerReceptionUseCase reception,
      LibererMaterielUseCase liberation,
      ReleverHeuresUseCase releve,
      ChangerEtatMaterielUseCase changementEtat,
      ReformerMaterielUseCase reforme,
      CurrentUserProvider utilisateurs) {
    this.affectation = affectation;
    this.reception = reception;
    this.liberation = liberation;
    this.releve = releve;
    this.changementEtat = changementEtat;
    this.reforme = reforme;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param id matériel
   * @param requete chantier et période
   * @return l'affectation créée
   */
  @PostMapping("/affecter")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Affecter un matériel à un chantier",
      description =
          "Règle R04 : le matériel doit être DISPONIBLE (422 MATERIEL_NON_DISPONIBLE). Une"
              + " affectation active chevauchant la période renvoie 409 CONFLIT_AFFECTATION (y"
              + " compris en cas de demandes simultanées). L'affectation est créée"
              + " EN_ATTENTE_RECEPTION ; émet MATERIEL_AFFECTE.")
  @ApiResponse(responseCode = "201", description = "Affectation créée")
  @ApiResponse(responseCode = "409", description = "CONFLIT_AFFECTATION ou CONFLIT_CONCURRENCE")
  @ApiResponse(
      responseCode = "422",
      description = "MATERIEL_NON_DISPONIBLE (R04), CHANTIER_TERMINE ou PERIODE_INVALIDE")
  public AffectationDto affecter(
      @Parameter(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id,
      @Valid @RequestBody AffecterRequest requete) {
    return ApiMapper.affectation(
        affectation.affecter(
            new AffecterMaterielCommande(
                id,
                requete.chantierId(),
                requete.besoinId(),
                requete.dateDebut(),
                requete.dateFinPrevue(),
                utilisateurs.require())));
  }

  /**
   * @param id matériel
   * @param requete affectation visée et observations (facultatif)
   * @return l'affectation en cours
   */
  @PostMapping("/confirmer-reception")
  @PreAuthorize(Acces.CHEF_OU_RM)
  @Operation(
      summary = "Confirmer la réception sur chantier",
      description =
          "Chef de chantier (uniquement sur ses chantiers) ou RM ; l'affectation passe EN_COURS ;"
              + " émet RECEPTION_CONFIRMEE.")
  @ApiResponse(responseCode = "200", description = "Affectation en cours")
  @ApiResponse(
      responseCode = "409",
      description = "Aucune affectation active ou réception déjà confirmée")
  public AffectationDto confirmerReception(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ConfirmerReceptionRequest requete) {
    ConfirmerReceptionRequest r =
        requete == null ? new ConfirmerReceptionRequest(null, null) : requete;
    return ApiMapper.affectation(
        reception.confirmer(
            new ConfirmerReceptionCommande(
                id, r.affectationId(), r.observations(), utilisateurs.require())));
  }

  /**
   * @param id matériel
   * @param requete date de fin et commentaire (facultatif)
   * @return l'affectation terminée
   */
  @PostMapping("/liberer")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Libérer un matériel",
      description =
          "Termine l'affectation active ; le matériel redevient DISPONIBLE (ou reste"
              + " EN_MAINTENANCE) ; émet AFFECTATION_TERMINEE et MATERIEL_ETAT_CHANGE.")
  @ApiResponse(responseCode = "200", description = "Affectation terminée")
  @ApiResponse(responseCode = "409", description = "Aucune affectation active")
  @ApiResponse(responseCode = "422", description = "Date de fin incohérente")
  public AffectationDto liberer(
      @PathVariable UUID id, @Valid @RequestBody(required = false) LibererRequest requete) {
    LibererRequest r = requete == null ? new LibererRequest(null, null) : requete;
    return ApiMapper.affectation(
        liberation.liberer(
            new LibererMaterielCommande(
                id, r.dateFinReelle(), r.commentaire(), utilisateurs.require())));
  }

  /**
   * @param id matériel
   * @param requete relevé
   * @return le matériel
   */
  @PostMapping("/heures")
  @PreAuthorize(Acces.CHEF_OU_RM)
  @Operation(
      summary = "Relever le compteur horaire",
      description = "Le relevé ne peut pas être inférieur au compteur actuel.")
  @ApiResponse(responseCode = "200", description = "Matériel mis à jour")
  @ApiResponse(responseCode = "422", description = "RELEVE_HEURES_INVALIDE ou matériel réformé")
  public MaterielDto releverHeures(
      @PathVariable UUID id, @Valid @RequestBody ReleveHeuresRequest requete) {
    return ApiMapper.materiel(
        releve.relever(id, requete.heuresFonctionnement(), utilisateurs.require()));
  }

  /**
   * @param id matériel
   * @param requete état demandé et motif
   * @return le matériel
   */
  @PatchMapping("/etat")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Changer l'état d'un matériel",
      description =
          "EN_MAINTENANCE (immobilisation) ou DISPONIBLE (remise en service : retour AFFECTE si"
              + " une affectation est en cours). Jamais REFORME ici. Émet MATERIEL_ETAT_CHANGE.")
  @ApiResponse(responseCode = "200", description = "Matériel mis à jour")
  @ApiResponse(responseCode = "409", description = "TRANSITION_INVALIDE")
  @ApiResponse(
      responseCode = "422",
      description = "ETAT_CIBLE_INTERDIT, AFFECTATION_EN_COURS, MAINTENANCE_EN_COURS")
  public MaterielDto changerEtat(
      @PathVariable UUID id, @Valid @RequestBody ChangementEtatRequest requete) {
    return ApiMapper.materiel(
        changementEtat.changerEtat(id, requete.etat(), requete.motif(), utilisateurs.require()));
  }

  /**
   * @param id matériel
   * @param requete motif
   * @return le matériel
   */
  @PostMapping("/reforme/demande")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Demander la réforme (R13, étape 1)",
      description = "Émet MATERIEL_REFORME_DEMANDEE ; le matériel n'est plus affectable.")
  @ApiResponse(responseCode = "200", description = "Demande enregistrée")
  @ApiResponse(responseCode = "409", description = "REFORME_DEJA_DEMANDEE")
  @ApiResponse(responseCode = "422", description = "Matériel déjà réformé")
  public MaterielDto demanderReforme(
      @PathVariable UUID id, @Valid @RequestBody DemandeReformeRequest requete) {
    return ApiMapper.materiel(reforme.demander(id, requete.motif(), utilisateurs.require()));
  }

  /**
   * @param id matériel
   * @param requete décision
   * @return le matériel
   */
  @PatchMapping("/reforme/valider")
  @PreAuthorize(Acces.DG)
  @Operation(
      summary = "Décider de la réforme (R13, étape 2)",
      description =
          "Direction Générale : approuvé → REFORME (terminal, émet MATERIEL_REFORME) ; refusé →"
              + " la demande est levée. Le matériel doit être libéré et hors maintenance.")
  @ApiResponse(responseCode = "200", description = "Décision appliquée")
  @ApiResponse(responseCode = "409", description = "REFORME_NON_DEMANDEE")
  @ApiResponse(responseCode = "422", description = "REFORME_IMPOSSIBLE (affecté ou en maintenance)")
  public MaterielDto validerReforme(
      @PathVariable UUID id, @Valid @RequestBody DecisionReformeRequest requete) {
    return ApiMapper.materiel(
        reforme.decider(id, requete.approuve(), requete.commentaire(), utilisateurs.require()));
  }
}

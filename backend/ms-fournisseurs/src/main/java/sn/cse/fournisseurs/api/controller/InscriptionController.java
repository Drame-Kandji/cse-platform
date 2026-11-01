package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.fournisseurs.api.dto.InscriptionDto;
import sn.cse.fournisseurs.api.dto.InscriptionRequest;
import sn.cse.fournisseurs.application.command.InscriptionCommand;
import sn.cse.fournisseurs.application.port.in.InscrireFournisseurUseCase;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/**
 * Auto-inscription publique d'un fournisseur (contrat §13) : seule route du service accessible sans
 * jeton ({@code cse.security.public-paths}) ; la passerelle la limite par adresse IP.
 */
@RestController
@RequestMapping(InscriptionController.CHEMIN)
@Tag(
    name = "Portail fournisseur",
    description = "Profil et catalogue du fournisseur connecté (claim fournisseur_id)")
public class InscriptionController {

  /** Chemin public de l'inscription. */
  public static final String CHEMIN = "/api/portail/inscription";

  private static final String EXEMPLE_409 =
      "{\"type\":\"about:blank\",\"title\":\"Conflit\",\"status\":409,\"detail\":\"Un fournisseur"
          + " est déjà enregistré avec ce NINEA ou cette adresse e-mail. Contactez le service des"
          + " achats de la CSE.\",\"code\":\"FOURNISSEUR_EXISTANT\",\"traceId\":"
          + "\"4bf92f3577b34da6a3ce929d0e0e4736\",\"timestamp\":\"2026-10-30T09:15:00Z\"}";

  private final InscrireFournisseurUseCase inscription;

  /**
   * @param inscription cas d'utilisation
   */
  public InscriptionController(InscrireFournisseurUseCase inscription) {
    this.inscription = inscription;
  }

  /**
   * @param requete données d'inscription
   * @return accusé de réception
   */
  @PostMapping
  @PreAuthorize(Acces.PUBLIC)
  @SecurityRequirements
  @Operation(
      summary = "S'inscrire comme fournisseur",
      description =
          "Route publique (sans jeton, limitée par adresse IP à la passerelle). Le fournisseur est"
              + " créé EN_ATTENTE_VALIDATION et l'événement FOURNISSEUR_INSCRIT est publié. Après"
              + " validation par le Responsable des Achats, un compte du portail est créé et un"
              + " e-mail de définition du mot de passe est envoyé à l'adresse indiquée.")
  @ApiResponse(responseCode = "201", description = "Inscription enregistrée")
  @ApiResponse(
      responseCode = "409",
      description = "NINEA ou e-mail déjà connu",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
              examples = @ExampleObject(name = "FOURNISSEUR_EXISTANT", value = EXEMPLE_409)))
  public ResponseEntity<InscriptionDto> inscrire(@Valid @RequestBody InscriptionRequest requete) {
    Fournisseur inscrit =
        inscription.inscrire(
            new InscriptionCommand(
                requete.raisonSociale(),
                requete.ninea(),
                requete.rccm(),
                requete.adresse(),
                requete.ville(),
                requete.pays(),
                requete.telephone(),
                requete.email(),
                requete.contactNom(),
                requete.contactFonction(),
                requete.specialites()));
    // Pas d'en-tête Location : la fiche fournisseur n'est pas lisible par un appelant anonyme.
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new InscriptionDto(
                inscrit.getId(), inscrit.getProfil().raisonSociale(), inscrit.getStatut()));
  }
}

package sn.cse.auth.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
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
import sn.cse.auth.api.dto.CreerUtilisateurRequest;
import sn.cse.auth.api.dto.ModifierUtilisateurRequest;
import sn.cse.auth.api.dto.MotDePasseRequest;
import sn.cse.auth.api.dto.StatutRequest;
import sn.cse.auth.api.dto.UtilisateurDto;
import sn.cse.auth.api.mapper.UtilisateurApiMapper;
import sn.cse.auth.application.command.ChangerStatutCommand;
import sn.cse.auth.application.command.ReinitialiserMotDePasseCommand;
import sn.cse.auth.application.port.in.ChangerStatutUtilisateurUseCase;
import sn.cse.auth.application.port.in.CreerUtilisateurUseCase;
import sn.cse.auth.application.port.in.ModifierUtilisateurUseCase;
import sn.cse.auth.application.port.in.RechercherUtilisateursUseCase;
import sn.cse.auth.application.port.in.ReinitialiserMotDePasseUseCase;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.domain.model.Role;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/** Administration des comptes utilisateurs (ADMIN_SYSTEME uniquement). */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@Tag(name = "Administration des utilisateurs", description = "Comptes Keycloak et profils (ADMIN)")
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
@ApiResponse(
    responseCode = "503",
    description = "Keycloak indisponible (IDENTITE_INDISPONIBLE)",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
public class AdminUtilisateurController {

  /** Champs triables (liste blanche). */
  static final Set<String> TRIABLES = Set.of("nom", "prenom", "email", "role", "createdAt");

  static final String TRI_DEFAUT = "nom,asc";

  private final RechercherUtilisateursUseCase rechercher;
  private final CreerUtilisateurUseCase creer;
  private final ModifierUtilisateurUseCase modifier;
  private final ChangerStatutUtilisateurUseCase changerStatut;
  private final ReinitialiserMotDePasseUseCase reinitialiser;
  private final UtilisateurApiMapper mapper;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param rechercher consultation
   * @param creer création
   * @param modifier modification
   * @param changerStatut activation / désactivation
   * @param reinitialiser réinitialisation du mot de passe
   * @param mapper conversion
   * @param pages pagination bornée
   * @param utilisateurs utilisateur courant
   */
  public AdminUtilisateurController(
      RechercherUtilisateursUseCase rechercher,
      CreerUtilisateurUseCase creer,
      ModifierUtilisateurUseCase modifier,
      ChangerStatutUtilisateurUseCase changerStatut,
      ReinitialiserMotDePasseUseCase reinitialiser,
      UtilisateurApiMapper mapper,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.rechercher = rechercher;
    this.creer = creer;
    this.modifier = modifier;
    this.changerStatut = changerStatut;
    this.reinitialiser = reinitialiser;
    this.mapper = mapper;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param role filtre rôle
   * @param actif filtre statut
   * @param q recherche libre
   * @param page page
   * @param size taille
   * @param sort tri
   * @return page d'utilisateurs
   */
  @GetMapping
  @Operation(
      summary = "Lister les utilisateurs",
      description = "Recherche paginée. Tri autorisé : nom, prenom, email, role, createdAt.")
  @ApiResponse(responseCode = "200", description = "Page d'utilisateurs")
  @ApiResponse(
      responseCode = "400",
      description = "Paramètre invalide (tri non autorisé…)",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public PageResponse<UtilisateurDto> lister(
      @Parameter(description = "Rôle", example = "CHEF_CHANTIER") @RequestParam(required = false)
          Role role,
      @Parameter(description = "Statut du compte", example = "true") @RequestParam(required = false)
          Boolean actif,
      @Parameter(description = "Recherche sur e-mail, prénom, nom", example = "diop")
          @RequestParam(required = false)
          String q,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "nom,asc") @RequestParam(required = false) String sort) {
    PageQuery requete = pages.query(page, size, sort, TRIABLES, TRI_DEFAUT);
    return rechercher
        .rechercher(new CriteresRecherche(role, actif, q), requete)
        .map(mapper::versDto);
  }

  /**
   * @param id identifiant
   * @return l'utilisateur
   */
  @GetMapping("/{id}")
  @Operation(summary = "Consulter un utilisateur")
  @ApiResponse(responseCode = "200", description = "Utilisateur")
  @ApiResponse(
      responseCode = "404",
      description = "Utilisateur introuvable",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public UtilisateurDto consulter(@PathVariable UUID id) {
    return mapper.versDto(rechercher.parId(id));
  }

  /**
   * @param requete données du compte
   * @return 201 avec le profil créé
   */
  @PostMapping
  @Operation(
      summary = "Créer un utilisateur",
      description =
          "Crée le compte Keycloak (mot de passe temporaire, changement exigé à la première"
              + " connexion, TOTP exigé pour les rôles concernés), attribue le rôle et enregistre"
              + " le profil. Publie UTILISATEUR_CREE.")
  @ApiResponse(responseCode = "201", description = "Utilisateur créé")
  @ApiResponse(
      responseCode = "400",
      description = "Données invalides",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "E-mail déjà utilisé (EMAIL_DEJA_UTILISE)",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Mot de passe non conforme ou attribut invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ResponseEntity<UtilisateurDto> creer(@Valid @RequestBody CreerUtilisateurRequest requete) {
    UtilisateurDto cree = mapper.versDto(creer.creer(mapper.versCommande(requete)));
    return ResponseEntity.created(URI.create("/api/admin/utilisateurs/" + cree.id())).body(cree);
  }

  /**
   * @param id compte
   * @param requete nouvelles valeurs
   * @return le profil modifié
   */
  @PutMapping("/{id}")
  @Operation(
      summary = "Modifier un utilisateur",
      description =
          "Met à jour identité, rôle et attributs (profil et Keycloak). Un administrateur ne peut"
              + " pas modifier son propre rôle. Publie UTILISATEUR_MODIFIE.")
  @ApiResponse(responseCode = "200", description = "Utilisateur modifié")
  @ApiResponse(
      responseCode = "400",
      description = "Données invalides",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Utilisateur introuvable",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "E-mail déjà utilisé",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Opération sur son propre compte interdite",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public UtilisateurDto modifier(
      @PathVariable UUID id, @Valid @RequestBody ModifierUtilisateurRequest requete) {
    return mapper.versDto(
        modifier.modifier(mapper.versCommande(id, requete, utilisateurs.require().id())));
  }

  /**
   * @param id compte
   * @param requete statut cible
   * @return le profil modifié
   */
  @PatchMapping("/{id}/statut")
  @Operation(
      summary = "Activer / désactiver un utilisateur",
      description = "Un compte désactivé ne peut plus se connecter. Publie UTILISATEUR_MODIFIE.")
  @ApiResponse(responseCode = "200", description = "Statut modifié")
  @ApiResponse(
      responseCode = "404",
      description = "Utilisateur introuvable",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Désactivation de son propre compte interdite",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public UtilisateurDto changerStatut(
      @PathVariable UUID id, @Valid @RequestBody StatutRequest requete) {
    return mapper.versDto(
        changerStatut.changerStatut(
            new ChangerStatutCommand(id, requete.actif(), utilisateurs.require().id())));
  }

  /**
   * @param id compte
   * @param requete mot de passe temporaire
   * @return 204
   */
  @PostMapping("/{id}/reinitialiser-mot-de-passe")
  @Operation(
      summary = "Réinitialiser le mot de passe",
      description = "Impose un mot de passe temporaire, à changer à la prochaine connexion.")
  @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé")
  @ApiResponse(
      responseCode = "404",
      description = "Utilisateur introuvable",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Mot de passe non conforme",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ResponseEntity<Void> reinitialiserMotDePasse(
      @PathVariable UUID id, @Valid @RequestBody MotDePasseRequest requete) {
    reinitialiser.reinitialiser(
        new ReinitialiserMotDePasseCommand(id, requete.motDePasseTemporaire()));
    return ResponseEntity.noContent().build();
  }
}

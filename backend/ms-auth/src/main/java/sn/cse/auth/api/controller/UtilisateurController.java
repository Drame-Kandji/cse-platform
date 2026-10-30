package sn.cse.auth.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.auth.application.port.in.GestionUtilisateurUseCase;
import sn.cse.auth.application.port.in.GestionUtilisateurUseCase.CreerUtilisateurCommand;
import sn.cse.auth.application.port.in.GestionUtilisateurUseCase.UtilisateurDto;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Administration Utilisateurs (ms-auth)", description = "Gestion des comptes utilisateurs et profils métier CSE")
public class UtilisateurController {

    private final GestionUtilisateurUseCase gestionUtilisateurUseCase;

    public record CreerUtilisateurRequest(
            @NotBlank @Schema(description = "Nom de famille", example = "Kandji") String nom,
            @NotBlank @Schema(description = "Prénom", example = "Drame") String prenom,
            @NotBlank @Email @Schema(description = "Adresse e-mail professionnelle", example = "drame.kandji@cse.sn") String email,
            @NotBlank @Schema(description = "Rôle CSE", example = "CHEF_CHANTIER") String role,
            @Schema(description = "UUID du chantier attribué (si Chef de Chantier)", example = "11111111-1111-1111-1111-111111111111") UUID chantierAttribueId,
            @Schema(description = "Périmètre parc (si Responsable Matériel)", example = "PARC_DAKAR") String perimetreParc
    ) {}

    public record StatutUtilisateurRequest(
            @Schema(description = "Statut actif ou inactif", example = "true") boolean actif
    ) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SYSTEME')")
    @Operation(summary = "Lister les utilisateurs", description = "Liste paginée des profils utilisateurs CSE")
    public ResponseEntity<PageResponse<UtilisateurDto>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UtilisateurDto> tous = gestionUtilisateurUseCase.listerTous();
        return ResponseEntity.ok(PageResponse.of(tous, page, size, tous.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SYSTEME')")
    @Operation(summary = "Consulter un utilisateur par ID", description = "Retourne le détail du profil utilisateur (ex: b1111111-1111-1111-1111-111111111111)")
    public ResponseEntity<UtilisateurDto> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(gestionUtilisateurUseCase.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id.toString())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SYSTEME')")
    @Operation(summary = "Créer un compte utilisateur", description = "Crée un nouveau profil utilisateur CSE")
    public ResponseEntity<UtilisateurDto> creer(@Valid @RequestBody CreerUtilisateurRequest request) {
        UtilisateurDto cree = gestionUtilisateurUseCase.creer(new CreerUtilisateurCommand(
                request.nom(),
                request.prenom(),
                request.email(),
                request.role(),
                request.chantierAttribueId(),
                request.perimetreParc()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN_SYSTEME')")
    @Operation(summary = "Activer / Désactiver un utilisateur", description = "Modifie l'état actif d'un compte utilisateur")
    public ResponseEntity<UtilisateurDto> changerStatut(@PathVariable UUID id,
                                                        @RequestBody StatutUtilisateurRequest request) {
        return ResponseEntity.ok(gestionUtilisateurUseCase.changerStatut(id, request.actif()));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-auth")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-auth"));
    }
}

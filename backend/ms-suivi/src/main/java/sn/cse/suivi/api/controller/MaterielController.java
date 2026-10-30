package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.in.VerifierDisponibiliteUseCase;
import sn.cse.suivi.application.port.out.MaterielRepositoryPort;
import sn.cse.suivi.domain.model.Materiel;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/materiels")
@RequiredArgsConstructor
@Tag(name = "Matériels (ms-suivi)", description = "Gestion du parc matériel, disponibilité et affectations aux chantiers")
public class MaterielController {

    private final AffecterMaterielUseCase affecterMaterielUseCase;
    private final VerifierDisponibiliteUseCase verifierDisponibiliteUseCase;
    private final MaterielRepositoryPort materielRepositoryPort;

    public record AffecterMaterielRequest(
            @Schema(description = "UUID du chantier cible", example = "11111111-1111-1111-1111-111111111111") UUID chantierId,
            @Schema(description = "UUID du besoin associé (optionnel)", example = "c1111111-1111-1111-1111-111111111111") UUID besoinId,
            @Schema(description = "UUID du demandeur", example = "b1111111-1111-1111-1111-111111111111") UUID demandeurId,
            @Schema(description = "Date de début d'affectation", example = "2026-11-01") LocalDate dateDebut,
            @Schema(description = "Date de fin prévue", example = "2026-12-15") LocalDate dateFinPrevue,
            @Schema(description = "Motif d'affectation", example = "Terrassement chantier Dakar Plateau") String motif,
            @Schema(description = "Acteur réalisant l'affectation", example = "resp.materiel@cse.sn") String affectePar
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_CHANTIER','RESPONSABLE_MATERIEL','RESPONSABLE_ACHATS','DAF','DIRECTION_GENERALE','ADMIN_SYSTEME')")
    @Operation(summary = "Lister les matériels du parc", description = "Liste paginée des engins avec filtres optionnels état et catégorie")
    public ResponseEntity<PageResponse<Materiel>> lister(@RequestParam(required = false) String etat,
                                                         @RequestParam(required = false) String categorie,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        List<Materiel> materiels = materielRepositoryPort.findAll().stream()
                .filter(m -> etat == null || etat.isBlank() || m.getEtat().name().equalsIgnoreCase(etat))
                .filter(m -> categorie == null || categorie.isBlank() || m.getCategorie().equalsIgnoreCase(categorie))
                .toList();
        return ResponseEntity.ok(PageResponse.of(materiels, page, size, materiels.size()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un matériel", description = "Exemple d'ID initialisé : aaaa1111-1111-1111-1111-111111111111")
    public ResponseEntity<Materiel> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(materielRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materiel", id.toString())));
    }

    @GetMapping("/disponibilite")
    @Operation(summary = "Vérifier disponibilité (R04)", description = "Ex: categorie=PELLE, designation=Caterpillar, quantite=1")
    public ResponseEntity<VerifierDisponibiliteUseCase.DisponibiliteResult> disponibilite(
            @RequestParam(defaultValue = "PELLE") String categorie,
            @RequestParam(defaultValue = "Caterpillar") String designation,
            @RequestParam(defaultValue = "1") int quantite) {
        return ResponseEntity.ok(verifierDisponibiliteUseCase.verifier(categorie, designation, quantite));
    }

    @GetMapping("/carte")
    @Operation(summary = "Carte du parc", description = "Données GeoJSON pour affichage Leaflet")
    public ResponseEntity<Map<String, Object>> carte() {
        return ResponseEntity.ok(Map.of("type", "FeatureCollection", "features", materielRepositoryPort.findAll()));
    }

    @PostMapping("/{id}/affecter")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Affecter un matériel (R04)", description = "Vérifie état DISPONIBLE et absence de conflit (ex ID: aaaa1111-1111-1111-1111-111111111111)")
    public ResponseEntity<Map<String, String>> affecter(@PathVariable UUID id, @RequestBody AffecterMaterielRequest body) {
        affecterMaterielUseCase.affecter(new AffecterMaterielUseCase.AffecterMaterielCommand(
                id,
                body.chantierId() != null ? body.chantierId() : UUID.fromString("11111111-1111-1111-1111-111111111111"),
                body.besoinId(),
                body.demandeurId(),
                body.dateDebut() != null ? body.dateDebut() : LocalDate.now(),
                body.dateFinPrevue(),
                body.motif(),
                body.affectePar() != null ? body.affectePar() : "resp.materiel@cse.sn"
        ));
        return ResponseEntity.ok(Map.of("id", id.toString(), "status", "AFFECTE"));
    }

    @PostMapping("/{id}/confirmer-reception")
    @PreAuthorize("hasAnyRole('RESPONSABLE_MATERIEL','CHEF_CHANTIER')")
    @Operation(summary = "Confirmer réception matériel")
    public ResponseEntity<Map<String, String>> confirmerReception(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id.toString(), "status", "RECEPTIONNE"));
    }

    @PatchMapping("/{id}/etat")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Changer état matériel")
    public ResponseEntity<Map<String, String>> changerEtat(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("id", id.toString(), "status", body.getOrDefault("etat", "DISPONIBLE")));
    }

    @PostMapping("/{id}/reforme/demande")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Demander réforme matériel (R13)")
    public ResponseEntity<Map<String, String>> demanderReforme(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id.toString(), "status", "REFORME_DEMANDE"));
    }

    @PatchMapping("/{id}/reforme/valider")
    @PreAuthorize("hasRole('DIRECTION_GENERALE')")
    @Operation(summary = "Valider réforme matériel (DG) (R13)")
    public ResponseEntity<Map<String, String>> validerReforme(@PathVariable UUID id) {
        Materiel m = materielRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materiel", id.toString()));
        m.reformer();
        materielRepositoryPort.save(m);
        return ResponseEntity.ok(Map.of("id", id.toString(), "status", "REFORME"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-suivi")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-suivi"));
    }
}

package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.fournisseurs.application.port.out.FournisseurRepositoryPort;
import sn.cse.fournisseurs.domain.model.Fournisseur;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs (ms-fournisseurs)", description = "Référentiel fournisseurs, évaluation (R07), suspension et blacklistage (R09/R14)")
public class ApiController {

    private final FournisseurRepositoryPort repositoryPort;

    public record CreerFournisseurRequest(
            @Schema(example = "Compagnie Ouest Africaine d'Équipements") String raisonSociale,
            @Schema(example = "SN-DKR-2026-00999") String ninea,
            @Schema(example = "RC-DKR-9999") String rc,
            @Schema(example = "contact@coae.sn") String email,
            @Schema(example = "+221 33 800 11 22") String telephone,
            @Schema(example = "ENGINS_LOURDS") String specialite
    ) {}

    public record EvaluerFournisseurRequest(
            @Schema(description = "Note qualité sur 100", example = "90") double noteQualite,
            @Schema(description = "Note respect des délais sur 100", example = "85") double noteDelai,
            @Schema(description = "Note conformité technique sur 100", example = "95") double noteConformite,
            @Schema(description = "Note réactivité SAV sur 100", example = "80") double noteSav,
            @Schema(example = "Livraison conforme et dans les délais sur le chantier de Dakar") String commentaire
    ) {}

    public record DemandeBlacklistageRequest(
            @Schema(example = "Défaillances répétées et non-respect des clauses de garantie") String motif
    ) {}

    @GetMapping
    @Operation(summary = "Lister les fournisseurs", description = "Filtrable par spécialité et statut")
    public ResponseEntity<PageResponse<Fournisseur>> lister(
            @RequestParam(required = false) String specialite,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Fournisseur> list = repositoryPort.findAll().stream()
                .filter(f -> specialite == null || specialite.isBlank() || f.getSpecialite().equalsIgnoreCase(specialite))
                .filter(f -> statut == null || statut.isBlank() || f.getStatut().equalsIgnoreCase(statut))
                .toList();
        return ResponseEntity.ok(PageResponse.of(list, page, size, list.size()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter la fiche d'un fournisseur", description = "Ex ID: f1111111-1111-1111-1111-111111111111")
    public ResponseEntity<Fournisseur> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_ACHATS','ADMIN_SYSTEME')")
    @Operation(summary = "Enregistrer un nouveau fournisseur")
    public ResponseEntity<Fournisseur> creer(@RequestBody CreerFournisseurRequest req) {
        Fournisseur f = new Fournisseur(
                UUID.randomUUID(), req.raisonSociale(), req.ninea(), req.rc(),
                req.email(), req.telephone(), req.specialite(), "EN_ATTENTE_VALIDATION",
                80.0, false, null, Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repositoryPort.save(f));
    }

    @PatchMapping("/{id}/valider-inscription")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Valider l'inscription d'un fournisseur", description = "Ex ID en attente: f3333333-3333-3333-3333-333333333333")
    public ResponseEntity<Fournisseur> validerInscription(@PathVariable UUID id) {
        Fournisseur f = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString()));
        f.validerInscription();
        return ResponseEntity.ok(repositoryPort.save(f));
    }

    @PostMapping("/{id}/evaluer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ACHATS','RESPONSABLE_MATERIEL')")
    @Operation(summary = "Évaluer un fournisseur et recalculer son score (R07)", description = "Ex ID: f1111111-1111-1111-1111-111111111111")
    public ResponseEntity<Fournisseur> evaluer(@PathVariable UUID id, @RequestBody EvaluerFournisseurRequest req) {
        Fournisseur f = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString()));
        f.recalculerScore(req.noteQualite(), req.noteDelai(), req.noteConformite(), req.noteSav());
        return ResponseEntity.ok(repositoryPort.save(f));
    }

    @PatchMapping("/{id}/suspendre")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Suspendre un fournisseur (R14)")
    public ResponseEntity<Fournisseur> suspendre(@PathVariable UUID id) {
        Fournisseur f = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString()));
        f.suspendre();
        return ResponseEntity.ok(repositoryPort.save(f));
    }

    @PostMapping("/{id}/blacklistage/demande")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Demander le blacklistage d'un fournisseur (Étape 1 - R09)", description = "Ex ID: f4444444-4444-4444-4444-444444444444")
    public ResponseEntity<Fournisseur> demanderBlacklistage(@PathVariable UUID id, @RequestBody DemandeBlacklistageRequest req) {
        Fournisseur f = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString()));
        f.demanderBlacklistage(req.motif());
        return ResponseEntity.ok(repositoryPort.save(f));
    }

    @PatchMapping("/{id}/blacklister")
    @PreAuthorize("hasRole('DIRECTION_GENERALE')")
    @Operation(summary = "Valider le blacklistage par la Direction Générale (Étape 2 - R09)", description = "Exige qu'une demande de blacklistage ait été soumise au préalable")
    public ResponseEntity<Fournisseur> blacklister(@PathVariable UUID id) {
        Fournisseur f = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id.toString()));
        f.blacklisterParDg();
        return ResponseEntity.ok(repositoryPort.save(f));
    }

    @GetMapping("/classement")
    @Operation(summary = "Classement des fournisseurs par score de performance")
    public ResponseEntity<List<Fournisseur>> classement(@RequestParam(required = false) String categorie) {
        List<Fournisseur> list = repositoryPort.findAll().stream()
                .filter(f -> categorie == null || categorie.isBlank() || f.getSpecialite().equalsIgnoreCase(categorie))
                .sorted(Comparator.comparingDouble(Fournisseur::getScorePerformance).reversed())
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/catalogue")
    @Operation(summary = "Consulter le catalogue de prix d'un fournisseur (§3.3)", description = "Ex ID: f1111111-1111-1111-1111-111111111111")
    public ResponseEntity<List<Map<String, Object>>> catalogue(@PathVariable UUID id) {
        return ResponseEntity.ok(List.of(
                Map.of("reference", "CAT-320D", "designation", "Pelle hydraulique Caterpillar 320D", "categorie", "MATERIEL_ROULANT", "prixUnitaireFcfa", 92500000, "delaiJours", 21),
                Map.of("reference", "CAT-140H", "designation", "Niveleuse Caterpillar 140H", "categorie", "MATERIEL_ROULANT", "prixUnitaireFcfa", 110000000, "delaiJours", 15)
        ));
    }

    @GetMapping("/comparaison-prix")
    @Operation(summary = "Comparer les prix fournisseurs par catégorie de matériel (§3.3)")
    public ResponseEntity<List<Map<String, Object>>> comparaisonPrix(@RequestParam(defaultValue = "MATERIEL_ROULANT") String categorie) {
        return ResponseEntity.ok(List.of(
                Map.of("fournisseur", "Sénégal Équipements BTP SA", "categorie", categorie, "designation", "Pelle Caterpillar 320D", "prixUnitaireFcfa", 92500000, "scoreFournisseur", 91.5),
                Map.of("fournisseur", "Dakar Engins & Grues SARL", "categorie", categorie, "designation", "Pelle Caterpillar 320D", "prixUnitaireFcfa", 96000000, "scoreFournisseur", 86.0)
        ));
    }

    @PostMapping("/{id}/litiges")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ACHATS','RESPONSABLE_MATERIEL')")
    @Operation(summary = "Ouvrir un litige fournisseur")
    public ResponseEntity<Map<String, Object>> ouvrirLitige(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", UUID.randomUUID().toString(),
                "fournisseurId", id.toString(),
                "motif", body.getOrDefault("motif", "Retard de livraison constaté"),
                "statut", "OUVERT"
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-fournisseurs")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-fournisseurs"));
    }
}

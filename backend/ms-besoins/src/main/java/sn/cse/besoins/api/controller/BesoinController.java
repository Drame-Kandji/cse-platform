package sn.cse.besoins.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.besoins.application.port.in.RejeterBesoinUseCase;
import sn.cse.besoins.application.port.in.SoumettreBesoinUseCase;
import sn.cse.besoins.application.port.in.ValiderBesoinUseCase;
import sn.cse.besoins.application.port.out.BesoinRepositoryPort;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/besoins")
@RequiredArgsConstructor
@Tag(name = "Besoins (ms-besoins)", description = "Expressions de besoins matériels des chantiers CSE")
public class BesoinController {

    private final SoumettreBesoinUseCase soumettreBesoinUseCase;
    private final ValiderBesoinUseCase validerBesoinUseCase;
    private final RejeterBesoinUseCase rejeterBesoinUseCase;
    private final BesoinRepositoryPort besoinRepositoryPort;

    public record SoumettreBesoinRequest(
            @Schema(description = "Désignation du matériel demandé", example = "Pelle hydraulique Caterpillar 320D") String designation,
            @Schema(description = "Quantité souhaitée", example = "2") int quantite,
            @Schema(description = "Spécifications techniques", example = "Godet 1.5m3, chenilles renforcées") String specifications,
            @Schema(description = "Priorité (NORMAL, URGENT, CRITIQUE)", example = "URGENT") Priorite priorite,
            @Schema(description = "UUID du chantier", example = "11111111-1111-1111-1111-111111111111") UUID chantierId,
            @Schema(description = "UUID du demandeur (Chef de chantier)", example = "b1111111-1111-1111-1111-111111111111") UUID demandeurId,
            @Schema(description = "Durée d'utilisation prévue en jours", example = "30") Integer dureeUtilisationPrevue,
            @Schema(description = "Justification (obligatoire si URGENT ou CRITIQUE - R08)", example = "Démarrage urgent terrassement Dakar Plateau") String justification
    ) {}

    public record ValiderBesoinRequest(
            @Schema(description = "UUID du Responsable Matériel validateur", example = "b2222222-2222-2222-2222-222222222222") UUID validePar
    ) {}

    public record RejeterBesoinRequest(
            @Schema(description = "Motif obligatoire du rejet (R08)", example = "Quantité surdimensionnée par rapport au planning du chantier") String motif,
            @Schema(description = "UUID du Responsable Matériel", example = "b2222222-2222-2222-2222-222222222222") UUID rejetePar
    ) {}

    @PostMapping
    @PreAuthorize("hasRole('CHEF_CHANTIER')")
    @Operation(summary = "Soumettre un besoin (R08)", description = "Création d'une demande de matériel. Justification obligatoire si URGENT/CRITIQUE")
    public ResponseEntity<SoumettreBesoinUseCase.SoumissionResult> creer(@RequestBody SoumettreBesoinRequest body) {
        SoumettreBesoinUseCase.SoumettreBesoinCommand cmd = new SoumettreBesoinUseCase.SoumettreBesoinCommand(
                body.designation(),
                body.quantite() > 0 ? body.quantite() : 1,
                body.specifications(),
                body.priorite() != null ? body.priorite() : Priorite.NORMAL,
                body.chantierId() != null ? body.chantierId() : UUID.fromString("11111111-1111-1111-1111-111111111111"),
                body.demandeurId() != null ? body.demandeurId() : UUID.fromString("b1111111-1111-1111-1111-111111111111"),
                body.dureeUtilisationPrevue(),
                body.justification()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(soumettreBesoinUseCase.soumettre(cmd));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_CHANTIER','RESPONSABLE_MATERIEL','DIRECTION_GENERALE','DAF','ADMIN_SYSTEME')")
    @Operation(summary = "Lister les besoins", description = "Liste paginée des besoins avec filtre optionnel par chantierId et statut")
    public ResponseEntity<PageResponse<BesoinMateriel>> lister(@RequestParam(required = false) UUID chantierId,
                                                               @RequestParam(required = false) String statut,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        List<BesoinMateriel> besoins = besoinRepositoryPort.findAll().stream()
                .filter(b -> chantierId == null || chantierId.equals(b.getChantierId()))
                .filter(b -> statut == null || statut.isBlank() || b.getStatut().name().equalsIgnoreCase(statut))
                .toList();
        return ResponseEntity.ok(PageResponse.of(besoins, page, size, besoins.size()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un besoin", description = "Exemple d'ID initialisé : c1111111-1111-1111-1111-111111111111")
    public ResponseEntity<BesoinMateriel> detail(@PathVariable UUID id) {
        BesoinMateriel besoin = besoinRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BesoinMateriel", id.toString()));
        return ResponseEntity.ok(besoin);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Valider un besoin (R01)", description = "Seul un besoin EN_ATTENTE peut être validé (ex: c1111111-1111-1111-1111-111111111111)")
    public ResponseEntity<Map<String, String>> valider(@PathVariable UUID id, @RequestBody(required = false) ValiderBesoinRequest body) {
        UUID validePar = body != null && body.validePar() != null ? body.validePar() : UUID.fromString("b2222222-2222-2222-2222-222222222222");
        validerBesoinUseCase.valider(id, validePar);
        return ResponseEntity.ok(Map.of("id", id.toString(), "statut", "VALIDE"));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Rejeter un besoin avec motif (R08)", description = "Motif obligatoire (ex: c3333333-3333-3333-3333-333333333333)")
    public ResponseEntity<Map<String, String>> rejeter(@PathVariable UUID id, @RequestBody RejeterBesoinRequest body) {
        UUID rejetePar = body.rejetePar() != null ? body.rejetePar() : UUID.fromString("b2222222-2222-2222-2222-222222222222");
        rejeterBesoinUseCase.rejeter(id, body.motif(), rejetePar);
        return ResponseEntity.ok(Map.of("id", id.toString(), "statut", "REJETE", "motif", body.motif() != null ? body.motif() : ""));
    }

    @GetMapping("/consolidation")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Consolidation des besoins par désignation")
    public ResponseEntity<List<BesoinMateriel>> consolidation() {
        return ResponseEntity.ok(besoinRepositoryPort.findAll());
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique validation")
    public ResponseEntity<List<Map<String, Object>>> historique(@PathVariable UUID id) {
        BesoinMateriel besoin = besoinRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BesoinMateriel", id.toString()));
        return ResponseEntity.ok(List.of(Map.of(
                "besoinId", besoin.getId(),
                "statutActuel", besoin.getStatut().name(),
                "dateCreation", besoin.getCreatedAt().toString()
        )));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-besoins")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-besoins"));
    }
}

package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.suivi.application.port.out.ChantierMaintenancePort;
import sn.cse.suivi.application.port.out.ChantierMaintenancePort.ChantierDto;
import sn.cse.suivi.application.port.out.MaterielRepositoryPort;
import sn.cse.suivi.domain.model.Materiel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api")
@Tag(name = "Chantiers, Maintenances & Pannes (ms-suivi)", description = "Déclaration de maintenance (§3.4), gestion des chantiers, pannes et planning d'affectations")
public class SuiviOperationsController {

    private final ChantierMaintenancePort chantierMaintenancePort;
    private final MaterielRepositoryPort materielRepositoryPort;

    public SuiviOperationsController(ChantierMaintenancePort chantierMaintenancePort, MaterielRepositoryPort materielRepositoryPort) {
        this.chantierMaintenancePort = chantierMaintenancePort;
        this.materielRepositoryPort = materielRepositoryPort;
    }

    public record DeclarerMaintenanceRequest(
            @Schema(description = "UUID du matériel concerné", example = "aaaa2222-2222-2222-2222-222222222222") UUID materielId,
            @Schema(description = "Type (PREVENTIVE, CORRECTIVE, LOURDE)", example = "PREVENTIVE") String typeMaintenance,
            @Schema(example = "2026-11-05") LocalDate datePlanifiee,
            @Schema(example = "Atelier Central CSE Dakar") String technicien,
            @Schema(example = "Révision des 1000 heures et remplacement des filtres hydrauliques") String motif,
            @Schema(example = "850000") BigDecimal coutEstime
    ) {}

    public record MaintenanceDto(
            UUID id,
            String numeroOt,
            UUID materielId,
            String typeMaintenance,
            String statut,
            LocalDate datePlanifiee,
            String technicien,
            String motif,
            BigDecimal cout
    ) {}

    public record SignalerPanneRequest(
            @Schema(example = "aaaa3333-3333-3333-3333-333333333333") UUID materielId,
            @Schema(example = "22222222-2222-2222-2222-222222222222") UUID chantierId,
            @Schema(example = "Fuite circuit hydraulique sur vérin de lame") String description,
            @Schema(example = "URGENTE") String urgence
    ) {}

    public record CreerChantierRequest(
            @Schema(example = "Chantier Touba VRD") String nom,
            @Schema(example = "Touba, Sénégal") String adresse,
            @Schema(example = "Touba") String ville,
            @Schema(example = "14.8500") BigDecimal latitude,
            @Schema(example = "-15.8833") BigDecimal longitude,
            @Schema(example = "550000000") BigDecimal budget
    ) {}

    private final List<MaintenanceDto> maintenances = new CopyOnWriteArrayList<>(List.of(
            new MaintenanceDto(
                    UUID.fromString("61111111-1111-1111-1111-111111111111"),
                    "OT-2026-001",
                    UUID.fromString("aaaa2222-2222-2222-2222-222222222222"),
                    "PREVENTIVE",
                    "PLANIFIEE",
                    LocalDate.of(2026, 11, 10),
                    "Équipe Maintenance CSE Dakar",
                    "Entretien préventif 1000h Bulldozer Komatsu D65EX",
                    BigDecimal.valueOf(750000)
            )
    ));

    @PostMapping("/maintenances")
    @PreAuthorize("hasAnyRole('RESPONSABLE_MATERIEL','CHEF_CHANTIER')")
    @Operation(summary = "Déclarer une intervention de maintenance (§3.4)", description = "Crée un Ordre de Travail (OT) et passe le matériel à l'état EN_MAINTENANCE")
    public ResponseEntity<MaintenanceDto> declarerMaintenance(@RequestBody DeclarerMaintenanceRequest req) {
        if (req.materielId() != null) {
            Materiel m = materielRepositoryPort.findById(req.materielId())
                    .orElseThrow(() -> new ResourceNotFoundException("Materiel", req.materielId().toString()));
            if (m.estAffectable()) {
                m.mettreEnMaintenance();
                materielRepositoryPort.save(m);
            }
        }
        MaintenanceDto dto = new MaintenanceDto(
                UUID.randomUUID(),
                "OT-" + (System.currentTimeMillis() % 100000),
                req.materielId(),
                req.typeMaintenance() != null ? req.typeMaintenance() : "PREVENTIVE",
                "EN_COURS",
                req.datePlanifiee() != null ? req.datePlanifiee() : LocalDate.now(),
                req.technicien(),
                req.motif(),
                req.coutEstime()
        );
        maintenances.add(0, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/maintenances")
    @Operation(summary = "Lister les interventions de maintenance")
    public ResponseEntity<List<MaintenanceDto>> listerMaintenances() {
        return ResponseEntity.ok(maintenances);
    }

    @PatchMapping("/maintenances/{id}/cloturer")
    @PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")
    @Operation(summary = "Clôturer une maintenance et remettre le matériel DISPONIBLE", description = "Ex ID: 61111111-1111-1111-1111-111111111111")
    public ResponseEntity<MaintenanceDto> cloturerMaintenance(@PathVariable UUID id) {
        for (int i = 0; i < maintenances.size(); i++) {
            MaintenanceDto m = maintenances.get(i);
            if (m.id().equals(id)) {
                if (m.materielId() != null) {
                    materielRepositoryPort.findById(m.materielId()).ifPresent(mat -> {
                        if ("EN_MAINTENANCE".equals(mat.getEtat().name())) {
                            mat.sortirMaintenance();
                            materielRepositoryPort.save(mat);
                        }
                    });
                }
                MaintenanceDto maj = new MaintenanceDto(
                        m.id(), m.numeroOt(), m.materielId(), m.typeMaintenance(),
                        "CLOTUREE", m.datePlanifiee(), m.technicien(), m.motif(), m.cout()
                );
                maintenances.set(i, maj);
                return ResponseEntity.ok(maj);
            }
        }
        throw new ResourceNotFoundException("Maintenance", id.toString());
    }

    @GetMapping("/chantiers")
    @Operation(summary = "Lister les chantiers CSE au Sénégal")
    public ResponseEntity<List<ChantierDto>> listerChantiers() {
        return ResponseEntity.ok(chantierMaintenancePort.findAllChantiers());
    }

    @PostMapping("/chantiers")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','RESPONSABLE_MATERIEL')")
    @Operation(summary = "Créer un nouveau chantier")
    public ResponseEntity<ChantierDto> creerChantier(@RequestBody CreerChantierRequest req) {
        ChantierDto dto = new ChantierDto(
                UUID.randomUUID(), req.nom(), req.adresse(), req.ville(),
                req.latitude(), req.longitude(), "EN_COURS", req.budget()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(chantierMaintenancePort.saveChantier(dto));
    }

    @PostMapping("/pannes")
    @PreAuthorize("hasAnyRole('CHEF_CHANTIER','RESPONSABLE_MATERIEL')")
    @Operation(summary = "Signaler une panne sur chantier (Chef de Chantier)")
    public ResponseEntity<Map<String, Object>> signalerPanne(@RequestBody SignalerPanneRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", UUID.randomUUID().toString(),
                "materielId", req.materielId() != null ? req.materielId().toString() : "",
                "chantierId", req.chantierId() != null ? req.chantierId().toString() : "",
                "urgence", req.urgence() != null ? req.urgence() : "NORMALE",
                "statut", "PANNE_SIGNALEE",
                "horodatage", Instant.now().toString()
        ));
    }

    @GetMapping("/affectations/planning")
    @Operation(summary = "Consulter le planning des affectations")
    public ResponseEntity<List<Map<String, Object>>> planning(@RequestParam(required = false) UUID chantierId) {
        return ResponseEntity.ok(List.of(Map.of(
                "materielImmatriculation", "CSE-PEL-001",
                "designation", "Pelle hydraulique Caterpillar 320D",
                "chantier", "Chantier Dakar Plateau",
                "dateDebut", "2026-11-01",
                "dateFinPrevue", "2026-12-15"
        )));
    }
}


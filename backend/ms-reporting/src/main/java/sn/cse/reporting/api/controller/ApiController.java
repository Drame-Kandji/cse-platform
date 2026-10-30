package sn.cse.reporting.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reporting")
@Tag(name = "Reporting & KPIs (ms-reporting)", description = "Tableaux de bord par profil (DG, RM, RA, DAF, Chef), séries temporelles et exports")
public class ApiController {

    @GetMapping("/kpis")
    @Operation(summary = "Consulter les KPIs par profil et période", description = "Profils : DIRECTION_GENERALE, RESPONSABLE_MATERIEL, RESPONSABLE_ACHATS, DAF, CHEF_CHANTIER")
    public ResponseEntity<Map<String, Object>> kpis(
            @Parameter(example = "DIRECTION_GENERALE") @RequestParam(defaultValue = "DIRECTION_GENERALE") String profil,
            @Parameter(example = "2026-Q4") @RequestParam(defaultValue = "2026-Q4") String periode) {
        return ResponseEntity.ok(Map.of(
                "profil", profil,
                "periode", periode,
                "tauxDisponibiliteParcPct", 84.5,
                "tauxUtilisationMoyennePct", 78.2,
                "delaiMoyenApprovisionnementJours", 18,
                "budgetEngageFcfa", 605000000L,
                "budgetRealiseFcfa", 412000000L,
                "chantiersActifs", 5,
                "enginsEnMaintenance", 1,
                "fournisseursActifs", 12
        ));
    }

    @GetMapping("/series")
    @Operation(summary = "Séries temporelles pour graphiques (Recharts)", description = "Ex: kpi=TAUX_DISPONIBILITE, granularite=MENSUEL")
    public ResponseEntity<List<Map<String, Object>>> series(
            @Parameter(example = "TAUX_DISPONIBILITE") @RequestParam(defaultValue = "TAUX_DISPONIBILITE") String kpi,
            @Parameter(example = "MENSUEL") @RequestParam(defaultValue = "MENSUEL") String granularite) {
        return ResponseEntity.ok(List.of(
                Map.of("periode", "2026-07", "kpi", kpi, "valeur", 81.0),
                Map.of("periode", "2026-08", "kpi", kpi, "valeur", 83.2),
                Map.of("periode", "2026-09", "kpi", kpi, "valeur", 82.5),
                Map.of("periode", "2026-10", "kpi", kpi, "valeur", 84.5)
        ));
    }

    @GetMapping("/export")
    @Operation(summary = "Exporter un rapport KPI (PDF ou XLSX)")
    public ResponseEntity<Map<String, Object>> export(
            @Parameter(example = "pdf") @RequestParam(defaultValue = "pdf") String format,
            @Parameter(example = "2026-Q4") @RequestParam(defaultValue = "2026-Q4") String periode) {
        return ResponseEntity.ok(Map.of(
                "status", "GENERATED",
                "format", format.toUpperCase(),
                "periode", periode,
                "fichier", "rapport-cse-" + periode + "." + format.toLowerCase()
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-reporting")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-reporting"));
    }
}

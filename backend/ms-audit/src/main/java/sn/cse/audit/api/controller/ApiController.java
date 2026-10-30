package sn.cse.audit.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Journal d'Audit (ms-audit)", description = "Journal d'audit immuable des actions métier (réservé ADMIN_SYSTEME)")
public class ApiController {

    public record AuditLogDto(
            UUID id,
            String utilisateur,
            String action,
            String entite,
            String entiteId,
            String details,
            Instant timestamp
    ) {}

    private final List<AuditLogDto> logs = List.of(
            new AuditLogDto(UUID.fromString("81111111-1111-1111-1111-111111111111"), "chef.chantier@cse.sn", "CREATION", "BesoinMateriel", "c1111111-1111-1111-1111-111111111111", "Soumission besoin BES-2026-001 (URGENT)", Instant.now().minusSeconds(7200)),
            new AuditLogDto(UUID.fromString("82222222-2222-2222-2222-222222222222"), "resp.materiel@cse.sn", "VALIDATION", "BesoinMateriel", "c2222222-2222-2222-2222-222222222222", "Validation besoin BES-2026-002", Instant.now().minusSeconds(5400)),
            new AuditLogDto(UUID.fromString("83333333-3333-3333-3333-333333333333"), "resp.achats@cse.sn", "PUBLICATION", "AppelOffre", "d1111111-1111-1111-1111-111111111111", "Publication AO-2026-001", Instant.now().minusSeconds(3600))
    );

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SYSTEME')")
    @Operation(summary = "Consulter le journal d'audit", description = "Filtrable par utilisateur, entite et action")
    public ResponseEntity<PageResponse<AuditLogDto>> lister(
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AuditLogDto> filtered = logs.stream()
                .filter(l -> utilisateur == null || utilisateur.isBlank() || l.utilisateur().equalsIgnoreCase(utilisateur))
                .filter(l -> entite == null || entite.isBlank() || l.entite().equalsIgnoreCase(entite))
                .filter(l -> action == null || action.isBlank() || l.action().equalsIgnoreCase(action))
                .toList();
        return ResponseEntity.ok(PageResponse.of(filtered, page, size, filtered.size()));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-audit")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-audit"));
    }
}

package sn.cse.notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications (ms-notification)", description = "Notifications in-app et e-mail (MailHog) pour les acteurs CSE")
public class ApiController {

    public record NotificationDto(
            UUID id,
            String destinataire,
            String type,
            String sujet,
            String message,
            boolean lue,
            Instant createdAt
    ) {}

    public record EnvoyerNotificationRequest(
            @Schema(example = "resp.materiel@cse.sn") String destinataire,
            @Schema(example = "BESOIN_URGENT_SOUMIS") String type,
            @Schema(example = "Nouveau besoin URGENT - Chantier Dakar Plateau") String sujet,
            @Schema(example = "Le besoin BES-2026-001 (2 Pelles Caterpillar 320D) est en attente de votre validation.") String message
    ) {}

    private final List<NotificationDto> notifications = new CopyOnWriteArrayList<>(List.of(
            new NotificationDto(
                    UUID.fromString("71111111-1111-1111-1111-111111111111"),
                    "resp.materiel@cse.sn",
                    "BESOIN_URGENT_SOUMIS",
                    "Besoin URGENT soumis (BES-2026-001)",
                    "Demande de 2 Pelles hydrauliques Caterpillar 320D pour le Chantier Dakar Plateau.",
                    false,
                    Instant.now().minusSeconds(1800)
            ),
            new NotificationDto(
                    UUID.fromString("72222222-2222-2222-2222-222222222222"),
                    "daf@cse.sn",
                    "ENGAGEMENT_A_VALIDER",
                    "Engagement budgétaire en attente (CTR-2026-001)",
                    "Le contrat CTR-2026-001 (110 000 000 FCFA) attend la validation d'engagement de la DAF.",
                    false,
                    Instant.now().minusSeconds(900)
            )
    ));

    @GetMapping
    @Operation(summary = "Lister les notifications")
    public ResponseEntity<List<NotificationDto>> lister(@RequestParam(required = false) String destinataire) {
        List<NotificationDto> res = notifications.stream()
                .filter(n -> destinataire == null || destinataire.isBlank() || n.destinataire().equalsIgnoreCase(destinataire))
                .toList();
        return ResponseEntity.ok(res);
    }

    @PostMapping
    @Operation(summary = "Envoyer une notification")
    public ResponseEntity<NotificationDto> envoyer(@RequestBody EnvoyerNotificationRequest req) {
        NotificationDto n = new NotificationDto(
                UUID.randomUUID(), req.destinataire(), req.type(), req.sujet(), req.message(), false, Instant.now()
        );
        notifications.add(0, n);
        return ResponseEntity.status(HttpStatus.CREATED).body(n);
    }

    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue", description = "Ex ID: 71111111-1111-1111-1111-111111111111")
    public ResponseEntity<NotificationDto> marquerLue(@PathVariable UUID id) {
        for (int i = 0; i < notifications.size(); i++) {
            NotificationDto n = notifications.get(i);
            if (n.id().equals(id)) {
                NotificationDto maj = new NotificationDto(n.id(), n.destinataire(), n.type(), n.sujet(), n.message(), true, n.createdAt());
                notifications.set(i, maj);
                return ResponseEntity.ok(maj);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-notification")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-notification"));
    }
}

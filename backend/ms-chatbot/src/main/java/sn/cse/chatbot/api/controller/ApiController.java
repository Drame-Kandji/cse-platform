package sn.cse.chatbot.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Assistant IA (ms-chatbot)", description = "Assistant conversationnel intelligent CSE (recherche engins, besoins, commandes, maintenance)")
public class ApiController {

    public record ChatbotQueryRequest(
            @Schema(description = "Question en langage naturel", example = "Quelles sont les pelles hydrauliques disponibles au parc de Dakar ?") String question
    ) {}

    public record ConversationEntry(
            UUID id,
            String question,
            String intentionDetectee,
            String reponse,
            Instant horodatage
    ) {}

    private final List<ConversationEntry> historique = new CopyOnWriteArrayList<>(List.of(
            new ConversationEntry(
                    UUID.fromString("91111111-1111-1111-1111-111111111111"),
                    "Quel est l'état du parc d'engins lourds à Dakar ?",
                    "DISPONIBILITE_MATERIEL",
                    "Le Parc Central de Dakar dispose actuellement de 3 engins disponibles : Pelle Caterpillar 320D (CSE-PEL-001), Bulldozer Komatsu D65EX (CSE-BUL-001) et Grue mobile Liebherr LTM 1055 (CSE-GRU-001).",
                    Instant.now().minusSeconds(3600)
            )
    ));

    @PostMapping("/query")
    @Operation(summary = "Poser une question à l'assistant IA CSE", description = "Analyse l'intention, filtre selon le rôle et génère une réponse contextualisée")
    public ResponseEntity<ConversationEntry> query(@RequestBody ChatbotQueryRequest req) {
        String q = req.question() != null ? req.question().toLowerCase() : "";
        String intention = "RECHERCHE_GENERALE";
        String reponse = "Assistant CSE : J'ai consulté les référentiels CSE. Cinq chantiers sont actifs (Dakar, Thiès, Saint-Louis, Kaolack, Ziguinchor) avec un taux de disponibilité du parc de 84,5 %.";

        if (q.contains("pelle") || q.contains("disponible") || q.contains("engin") || q.contains("parc")) {
            intention = "DISPONIBILITE_MATERIEL";
            reponse = "D'après ms-suivi, la Pelle hydraulique Caterpillar 320D (CSE-PEL-001, 1250h) et le Bulldozer Komatsu D65EX (CSE-BUL-001, 980h) sont actuellement DISPONIBLES au Parc Central de Dakar.";
        } else if (q.contains("besoin") || q.contains("demande")) {
            intention = "SUIVI_BESOINS";
            reponse = "D'après ms-besoins, 2 demandes sont EN_ATTENTE de validation (BES-2026-001 : 2 Pelles Caterpillar 320D pour Dakar Plateau ; BES-2026-003 : 1 Grue Liebherr 50T pour Saint-Louis) et 1 demande est VALIDÉE (BES-2026-002).";
        } else if (q.contains("appel") || q.contains("offre") || q.contains("contrat") || q.contains("fournisseur")) {
            intention = "SUIVI_ACHATS_FOURNISSEURS";
            reponse = "D'après ms-achats et ms-fournisseurs, l'appel d'offres AO-2026-001 est publié, le contrat CTR-2026-001 (110 000 000 FCFA) est signé avec Sénégal Équipements BTP SA (score : 91.5/100).";
        }

        ConversationEntry entry = new ConversationEntry(UUID.randomUUID(), req.question(), intention, reponse, Instant.now());
        historique.add(0, entry);
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/historique")
    @Operation(summary = "Consulter l'historique des conversations")
    public ResponseEntity<List<ConversationEntry>> getHistorique() {
        return ResponseEntity.ok(historique);
    }

    @DeleteMapping("/historique/{id}")
    @Operation(summary = "Supprimer une entrée de l'historique", description = "Ex ID: 91111111-1111-1111-1111-111111111111")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable UUID id) {
        historique.removeIf(e -> e.id().equals(id));
        return ResponseEntity.ok(Map.of("deleted", id.toString()));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-chatbot")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-chatbot"));
    }
}

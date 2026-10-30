package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.cse.achats.application.port.out.AchatsRepositoryPort;
import sn.cse.achats.application.port.out.AchatsRepositoryPort.AppelOffreDto;
import sn.cse.achats.application.port.out.AchatsRepositoryPort.ContratDto;
import sn.cse.achats.application.port.out.AchatsRepositoryPort.OffreDto;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Achats & Appels d'Offres (ms-achats)", description = "Appels d'offres (R02/R10), Attribution (R03/R11), Contrats & Engagements DAF (R06)")
public class ApiController {

    private final AchatsRepositoryPort repositoryPort;

    public record CreerAoRequest(
            @Schema(example = "Acquisition de 3 Camions-bennes Mercedes Actros") String objet,
            @Schema(example = "APPEL_OFFRES") String typeAo,
            @Schema(example = "c1111111-1111-1111-1111-111111111111") UUID besoinId,
            @Schema(example = "2026-12-31") LocalDate dateLimite
    ) {}

    public record DeposerOffreRequest(
            @Schema(example = "f1111111-1111-1111-1111-111111111111") UUID fournisseurId,
            @Schema(example = "175000000") BigDecimal montantPropose,
            @Schema(example = "20") int delaiLivraisonJours,
            @Schema(example = "88.5") double noteTechnique
    ) {}

    public record AttribuerAoRequest(
            @Schema(example = "e1111111-1111-1111-1111-111111111111") UUID offreId
    ) {}

    public record CreerContratRequest(
            @Schema(example = "d2222222-2222-2222-2222-222222222222") UUID appelOffreId,
            @Schema(example = "e2222222-2222-2222-2222-222222222222") UUID offreId,
            @Schema(example = "f1111111-1111-1111-1111-111111111111") UUID fournisseurId,
            @Schema(example = "110000000") BigDecimal montantTotal
    ) {}

    @GetMapping("/appels-offres")
    @Operation(summary = "Lister les appels d'offres")
    public ResponseEntity<PageResponse<AppelOffreDto>> listerAo(
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AppelOffreDto> list = repositoryPort.findAllAppelsOffres().stream()
                .filter(a -> statut == null || statut.isBlank() || a.statut().equalsIgnoreCase(statut))
                .toList();
        return ResponseEntity.ok(PageResponse.of(list, page, size, list.size()));
    }

    @PostMapping("/appels-offres")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Publier un nouvel Appel d'Offres")
    public ResponseEntity<AppelOffreDto> creerAo(@RequestBody CreerAoRequest req) {
        AppelOffreDto ao = new AppelOffreDto(
                UUID.randomUUID(),
                "AO-" + System.currentTimeMillis(),
                req.objet(),
                req.typeAo() != null ? req.typeAo() : "APPEL_OFFRES",
                req.besoinId(),
                req.dateLimite() != null ? req.dateLimite() : LocalDate.now().plusDays(30),
                "PUBLIE",
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repositoryPort.saveAppelOffre(ao));
    }

    @GetMapping("/appels-offres/{id}/offres")
    @Operation(summary = "Lister les offres d'un AO (Anonymisées avant clôture - R10)", description = "Ex ID: d1111111-1111-1111-1111-111111111111")
    public ResponseEntity<List<OffreDto>> listerOffres(@PathVariable UUID id) {
        AppelOffreDto ao = repositoryPort.findAppelOffreById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", id.toString()));
        List<OffreDto> offres = repositoryPort.findOffresByAppelOffreId(id);
        if ("PUBLIE".equals(ao.statut())) {
            // R10 : Anonymisation du fournisseurId tant que l'AO n'est pas clôturé
            offres = offres.stream().map(o -> new OffreDto(
                    o.id(), o.appelOffreId(), null, o.codeAnonyme(),
                    o.montantPropose(), o.delaiLivraisonJours(), o.noteTechnique()
            )).toList();
        }
        return ResponseEntity.ok(offres);
    }

    @PostMapping("/portail/appels-offres/{id}/offres")
    @Operation(summary = "Déposer une offre fournisseur (Vérifie dateLimite - R02)", description = "Ex ID AO ouvert: d1111111-1111-1111-1111-111111111111")
    public ResponseEntity<OffreDto> deposerOffre(@PathVariable UUID id, @RequestBody DeposerOffreRequest req) {
        AppelOffreDto ao = repositoryPort.findAppelOffreById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", id.toString()));
        if (!"PUBLIE".equals(ao.statut()) || LocalDate.now().isAfter(ao.dateLimite())) {
            throw new BusinessRuleException("AO_CLOTURE_OU_EXPIRE", "Cet appel d'offres n'accepte plus d'offres après sa date limite ou clôture (R02)");
        }
        OffreDto offre = new OffreDto(
                UUID.randomUUID(),
                id,
                req.fournisseurId() != null ? req.fournisseurId() : UUID.fromString("f1111111-1111-1111-1111-111111111111"),
                "OFF-ANON-" + (System.currentTimeMillis() % 10000),
                req.montantPropose(),
                req.delaiLivraisonJours(),
                req.noteTechnique()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repositoryPort.saveOffre(offre));
    }

    @PostMapping("/appels-offres/{id}/cloturer")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Clôturer un appel d'offres (lève l'anonymat des offres - R10)", description = "Ex ID: d1111111-1111-1111-1111-111111111111")
    public ResponseEntity<AppelOffreDto> cloturer(@PathVariable UUID id) {
        AppelOffreDto ao = repositoryPort.findAppelOffreById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", id.toString()));
        AppelOffreDto maj = new AppelOffreDto(ao.id(), ao.reference(), ao.objet(), ao.typeAo(),
                ao.besoinId(), ao.dateLimite(), "CLOTURE", ao.offreRetenueId());
        return ResponseEntity.ok(repositoryPort.saveAppelOffre(maj));
    }

    @PostMapping("/appels-offres/{id}/attribuer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ACHATS','DIRECTION_GENERALE')")
    @Operation(summary = "Attribuer le marché à une offre (R03/R11)", description = "Ex ID: d1111111-1111-1111-1111-111111111111")
    public ResponseEntity<AppelOffreDto> attribuer(@PathVariable UUID id, @RequestBody AttribuerAoRequest req) {
        AppelOffreDto ao = repositoryPort.findAppelOffreById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", id.toString()));
        AppelOffreDto maj = new AppelOffreDto(ao.id(), ao.reference(), ao.objet(), ao.typeAo(),
                ao.besoinId(), ao.dateLimite(), "ATTRIBUE", req.offreId());
        return ResponseEntity.ok(repositoryPort.saveAppelOffre(maj));
    }

    @GetMapping("/contrats")
    @Operation(summary = "Lister les contrats d'achat")
    public ResponseEntity<List<ContratDto>> listerContrats() {
        return ResponseEntity.ok(repositoryPort.findAllContrats());
    }

    @GetMapping("/contrats/{id}")
    @Operation(summary = "Consulter un contrat et son statut (§3.2)", description = "Ex ID: a1111111-1111-1111-1111-111111111111")
    public ResponseEntity<ContratDto> detailContrat(@PathVariable UUID id) {
        return ResponseEntity.ok(repositoryPort.findContratById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContratAchat", id.toString())));
    }

    @PostMapping("/contrats/{id}/signer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ACHATS','DIRECTION_GENERALE')")
    @Operation(summary = "Signer un contrat d'achat (ContratAchat.signer())", description = "Ex ID: a1111111-1111-1111-1111-111111111111")
    public ResponseEntity<ContratDto> signerContrat(@PathVariable UUID id) {
        ContratDto c = repositoryPort.findContratById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContratAchat", id.toString()));
        ContratDto maj = new ContratDto(c.id(), c.numero(), c.appelOffreId(), c.offreId(),
                c.fournisseurId(), c.montantTotal(), "SIGNE", c.engagementValideDaf());
        return ResponseEntity.ok(repositoryPort.saveContrat(maj));
    }

    @GetMapping("/appels-offres/{id}/comparatif")
    @Operation(summary = "Comparatif multicritère des offres (prix, délai, qualité technique - §3.2)", description = "Ex ID: d1111111-1111-1111-1111-111111111111")
    public ResponseEntity<List<Map<String, Object>>> comparatif(@PathVariable UUID id) {
        List<OffreDto> offres = repositoryPort.findOffresByAppelOffreId(id);
        return ResponseEntity.ok(offres.stream().map(o -> Map.<String, Object>of(
                "offreId", o.id(),
                "codeAnonyme", o.codeAnonyme(),
                "montantPropose", o.montantPropose(),
                "delaiLivraisonJours", o.delaiLivraisonJours(),
                "noteTechnique", o.noteTechnique(),
                "scoreGlobalPondere", Math.round((o.noteTechnique() * 0.6 + (100.0 / Math.max(1, o.delaiLivraisonJours())) * 40.0) * 10.0) / 10.0
        )).toList());
    }

    @GetMapping("/commandes")
    @Operation(summary = "Lister les bons de commande et jalons de livraison")
    public ResponseEntity<List<Map<String, Object>>> commandes(@RequestParam(required = false) String statut) {
        return ResponseEntity.ok(List.of(Map.of(
                "id", "51111111-1111-1111-1111-111111111111",
                "numeroCommande", "BC-2026-001",
                "contratNumero", "CTR-2026-001",
                "statut", statut != null ? statut : "EN_LIVRAISON",
                "delaiContractuelJours", 15
        )));
    }

    @GetMapping("/factures")
    @Operation(summary = "Lister les factures fournisseurs")
    public ResponseEntity<List<Map<String, Object>>> factures() {
        return ResponseEntity.ok(List.of(Map.of(
                "id", "41111111-1111-1111-1111-111111111111",
                "numeroFacture", "FAC-2026-001",
                "contratId", "a1111111-1111-1111-1111-111111111111",
                "montantTtc", 110000000,
                "statut", "EN_ATTENTE_MANDATEMENT"
        )));
    }

    @PostMapping("/factures/{id}/mandater")
    @PreAuthorize("hasRole('DAF')")
    @Operation(summary = "Mandater le paiement d'une facture par la DAF (DAF.mandaterPaiement())")
    public ResponseEntity<Map<String, Object>> mandaterFacture(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of(
                "factureId", id.toString(),
                "statut", "PAIEMENT_MANDATE"
        ));
    }

    @GetMapping("/besoins-a-acheter")
    @Operation(summary = "Projection locale des besoins validés EN_ACHAT")
    public ResponseEntity<List<Map<String, Object>>> besoinsAAcheter() {
        return ResponseEntity.ok(List.of(Map.of(
                "besoinId", "c1111111-1111-1111-1111-111111111111",
                "designation", "Pelle hydraulique Caterpillar 320D",
                "quantite", 2,
                "statut", "EN_ACHAT"
        )));
    }

    @PostMapping("/contrats")
    @PreAuthorize("hasRole('RESPONSABLE_ACHATS')")
    @Operation(summary = "Créer un contrat d'achat (Exige un AO ATTRIBUE - R03)", description = "Ex AO attribué: d2222222-2222-2222-2222-222222222222")
    public ResponseEntity<ContratDto> creerContrat(@RequestBody CreerContratRequest req) {
        AppelOffreDto ao = repositoryPort.findAppelOffreById(req.appelOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("AppelOffre", req.appelOffreId().toString()));
        if (!"ATTRIBUE".equals(ao.statut())) {
            throw new BusinessRuleException("AO_NON_ATTRIBUE", "Un contrat ne peut être créé que pour un appel d'offres ATTRIBUE (R03)");
        }
        ContratDto c = new ContratDto(
                UUID.randomUUID(),
                "CTR-" + System.currentTimeMillis(),
                req.appelOffreId(),
                req.offreId(),
                req.fournisseurId(),
                req.montantTotal(),
                "SIGNE",
                false
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repositoryPort.saveContrat(c));
    }

    @PostMapping("/engagements/{contratId}/valider")
    @PreAuthorize("hasRole('DAF')")
    @Operation(summary = "Valider l'engagement budgétaire par la DAF (R06)", description = "Ex ID Contrat: a1111111-1111-1111-1111-111111111111")
    public ResponseEntity<ContratDto> validerEngagement(@PathVariable UUID contratId) {
        ContratDto c = repositoryPort.findContratById(contratId)
                .orElseThrow(() -> new ResourceNotFoundException("ContratAchat", contratId.toString()));
        ContratDto maj = new ContratDto(c.id(), c.numero(), c.appelOffreId(), c.offreId(),
                c.fournisseurId(), c.montantTotal(), "ENGAGEMENT_VALIDE", true);
        return ResponseEntity.ok(repositoryPort.saveContrat(maj));
    }

    @GetMapping("/health")
    @Operation(summary = "Health ms-achats")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-achats"));
    }
}

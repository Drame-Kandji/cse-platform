package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Synthèse financière.
 *
 * @param montantEngage engagements validés
 * @param montantEngagementsEnAttente engagements en attente
 * @param montantFacture factures non rejetées (TTC)
 * @param montantMandate factures mandatées ou payées (TTC)
 * @param montantPaye factures payées (TTC)
 * @param facturesEnRetard nombre de factures en retard
 * @param engagementsEnAttente nombre d'engagements en attente
 */
@Schema(name = "SyntheseFinance", description = "Synthèse financière des achats (montants en FCFA)")
public record SyntheseFinanceDto(
    @Schema(description = "Engagements validés par la DAF", example = "812000000")
        BigDecimal montantEngage,
    @Schema(description = "Engagements en attente de la DAF", example = "164000000")
        BigDecimal montantEngagementsEnAttente,
    @Schema(description = "Factures non rejetées (TTC)", example = "530000000")
        BigDecimal montantFacture,
    @Schema(description = "Factures mandatées ou payées (TTC)", example = "410000000")
        BigDecimal montantMandate,
    @Schema(description = "Factures payées (TTC)", example = "350000000") BigDecimal montantPaye,
    @Schema(example = "2") long facturesEnRetard,
    @Schema(example = "2") long engagementsEnAttente) {}

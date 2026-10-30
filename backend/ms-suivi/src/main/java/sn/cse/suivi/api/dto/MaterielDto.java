package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/**
 * Matériel du parc.
 *
 * @param id identifiant
 * @param code code parc
 * @param immatriculation immatriculation
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin
 * @param typeEnginLibelle libellé du type
 * @param marque marque
 * @param modele modèle
 * @param etat état
 * @param heuresFonctionnement compteur horaire
 * @param heuresDepuisMaintenance heures depuis la dernière maintenance
 * @param seuilMaintenanceHeures seuil horaire de maintenance préventive
 * @param prochaineMaintenance échéance calendaire de maintenance
 * @param maintenanceDue seuil atteint
 * @param tarifJournalier coût journalier (FCFA)
 * @param valeurAcquisition valeur d'achat (FCFA)
 * @param dateAcquisition date d'achat
 * @param chantierActuelId chantier courant
 * @param chantierActuelNom nom du chantier courant
 * @param latitude latitude
 * @param longitude longitude
 * @param fournisseurId fournisseur d'origine
 * @param contratId contrat d'origine
 * @param reformeDemandee demande de réforme en attente
 * @param motifReforme motif de réforme
 * @param version version (verrou optimiste)
 */
@Schema(name = "Materiel", description = "Matériel du parc")
public record MaterielDto(
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID id,
    @Schema(example = "PEL-CAT320-01") String code,
    @Schema(example = "DK-4521-BH", nullable = true) String immatriculation,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String designation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "e0000000-0000-0000-0000-000000000001", nullable = true) UUID typeEnginId,
    @Schema(example = "Pelle hydraulique", nullable = true) String typeEnginLibelle,
    @Schema(example = "Caterpillar", nullable = true) String marque,
    @Schema(example = "320 GC", nullable = true) String modele,
    @Schema(example = "AFFECTE") EtatMateriel etat,
    @Schema(example = "4820") long heuresFonctionnement,
    @Schema(example = "312") long heuresDepuisMaintenance,
    @Schema(example = "500", nullable = true) Integer seuilMaintenanceHeures,
    @Schema(example = "2026-12-15", nullable = true) LocalDate prochaineMaintenance,
    @Schema(example = "false") boolean maintenanceDue,
    @Schema(description = "Coût journalier en FCFA", example = "350000") BigDecimal tarifJournalier,
    @Schema(description = "Valeur d'achat en FCFA", example = "145000000", nullable = true)
        BigDecimal valeurAcquisition,
    @Schema(example = "2023-03-14", nullable = true) LocalDate dateAcquisition,
    @Schema(example = "c0000000-0000-0000-0000-000000000001", nullable = true)
        UUID chantierActuelId,
    @Schema(example = "Extension autoroute Dakar–AIBD", nullable = true) String chantierActuelNom,
    @Schema(example = "14.7171", nullable = true) Double latitude,
    @Schema(example = "-17.4682", nullable = true) Double longitude,
    @Schema(example = "f0000000-0000-0000-0000-000000000001", nullable = true) UUID fournisseurId,
    @Schema(example = "4d2a6e8f-1b3c-4e5a-9f7d-2c4b6a8e0f1d", nullable = true) UUID contratId,
    @Schema(example = "false") boolean reformeDemandee,
    @Schema(example = "Châssis fissuré, réparation non rentable", nullable = true)
        String motifReforme,
    @Schema(example = "5") Long version) {}

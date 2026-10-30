package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.cse.suivi.domain.model.EtatMateriel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "materiels", schema = "suivi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielEntity {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String immatriculation;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String categorie;

    private String marque;
    private String modele;
    private Integer anneeFabrication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatMateriel etat;

    private Long heuresFonctionnement;
    private String localisation;
    private UUID chantierActuelId;
    private UUID fournisseurId;
    private UUID contratId;
    private LocalDate dateAcquisition;
    private BigDecimal valeurAcquisition;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @Version
    private Long version;
}

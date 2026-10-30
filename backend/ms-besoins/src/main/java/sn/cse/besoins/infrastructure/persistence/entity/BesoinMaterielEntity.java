package sn.cse.besoins.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "besoins_materiel", schema = "besoins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BesoinMaterielEntity {
    @Id
    private UUID id;

    private String reference;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private Integer quantite;

    private String specifications;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutBesoin statut;

    @Column(nullable = false)
    private UUID chantierId;

    @Column(nullable = false)
    private UUID demandeurId;

    private Integer dureeUtilisationPrevue;
    private String justification;
    private String motifRejet;

    private UUID validePar;
    private Instant dateValidation;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}

package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Fournisseur invité à un appel d'offres. */
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class InviteEmbeddable {

  @Column(name = "fournisseur_id", nullable = false)
  private UUID fournisseurId;

  @Column(name = "raison_sociale", nullable = false)
  private String raisonSociale;
}

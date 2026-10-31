package sn.cse.notification.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Table {@code annuaire_utilisateurs}. */
@Entity
@Table(name = "annuaire_utilisateurs")
public class UtilisateurAnnuaireEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(length = 100)
  private String prenom;

  @Column(length = 100)
  private String nom;

  @Column(nullable = false, length = 40)
  private String role;

  @Column(nullable = false)
  private boolean actif;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "annuaire_utilisateur_chantiers",
      joinColumns = @JoinColumn(name = "utilisateur_id"))
  @Column(name = "chantier_id", nullable = false)
  private Set<UUID> chantierIds = new HashSet<>();

  @Column(name = "mis_a_jour_le", nullable = false)
  private Instant misAJourLe;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected UtilisateurAnnuaireEntity() {}

  UtilisateurAnnuaireEntity(UUID id, Instant maintenant) {
    this.id = id;
    this.createdAt = maintenant;
  }

  void appliquer(UtilisateurAnnuaire u, Instant maintenant) {
    email = u.email() == null ? "" : u.email();
    prenom = u.prenom();
    nom = u.nom();
    role = u.role() == null ? "" : u.role();
    actif = u.actif();
    chantierIds.clear();
    chantierIds.addAll(u.chantierIds());
    misAJourLe = u.misAJourLe();
    updatedAt = maintenant;
  }

  UtilisateurAnnuaire versDomaine() {
    return new UtilisateurAnnuaire(
        id, email, prenom, nom, role, Set.copyOf(chantierIds), actif, misAJourLe);
  }
}

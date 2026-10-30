package sn.cse.auth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/** Jeux de données partagés par les tests unitaires. */
public final class Fixtures {

  public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  public static final UUID CHANTIER_1 = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  public static final UUID CHANTIER_2 = UUID.fromString("c0000000-0000-0000-0000-000000000002");
  public static final UUID FOURNISSEUR = UUID.fromString("f0000000-0000-0000-0000-000000000001");
  public static final Instant CREATION = Instant.parse("2026-01-15T08:00:00Z");

  public static final Utilisateur.Identite IDENTITE =
      new Utilisateur.Identite("chef.chantier@cse.sn", "Mamadou", "Diop", "+221 77 000 00 00");

  /** Attributs renseignés pour tous les profils à la fois. */
  public static final AttributsProfil TOUS_ATTRIBUTS =
      new AttributsProfil(
          Set.of(CHANTIER_1, CHANTIER_2),
          "NATIONAL",
          new BigDecimal("150000000"),
          new BigDecimal("5000000000"),
          FOURNISSEUR);

  private Fixtures() {}

  public static Utilisateur.EtatCommun etat(UUID id) {
    return new Utilisateur.EtatCommun(id, IDENTITE, true, false, CREATION);
  }

  public static Utilisateur utilisateur(Role role) {
    return Utilisateurs.creer(role, etat(ID), TOUS_ATTRIBUTS);
  }
}

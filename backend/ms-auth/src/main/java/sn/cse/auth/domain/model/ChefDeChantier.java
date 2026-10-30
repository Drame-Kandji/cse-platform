package sn.cse.auth.domain.model;

import java.util.Set;
import java.util.UUID;

/** Chef de chantier : n'accède qu'aux données des chantiers qui lui sont attribués. */
public final class ChefDeChantier extends Utilisateur {

  private Set<UUID> chantierIds;

  /**
   * @param etat état commun
   * @param chantierIds chantiers attribués (éventuellement aucun)
   */
  public ChefDeChantier(EtatCommun etat, Set<UUID> chantierIds) {
    super(etat);
    this.chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
  }

  @Override
  public Role role() {
    return Role.CHEF_CHANTIER;
  }

  @Override
  public AttributsProfil attributs() {
    return new AttributsProfil(chantierIds, null, null, null, null);
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    this.chantierIds = attributs.chantierIds();
  }

  /**
   * @param chantierId chantier visé
   * @return vrai si le chantier est attribué à ce chef
   */
  public boolean estAffecteAu(UUID chantierId) {
    return chantierIds.contains(chantierId);
  }

  /**
   * @return chantiers attribués (immuable)
   */
  public Set<UUID> chantierIds() {
    return chantierIds;
  }
}

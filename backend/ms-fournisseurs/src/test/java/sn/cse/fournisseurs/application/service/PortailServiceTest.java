package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.support.Fixtures;

class PortailServiceTest {

  private final Contexte ctx = new Contexte();
  private final PortailService portail =
      new PortailService(ctx.chargeur, ctx.referentiel, ctx.catalogues);

  private static CurrentUser compte(UUID fournisseurId) {
    return new CurrentUser(
        UUID.randomUUID(),
        "Moussa Kane",
        "fournisseur@cse.sn",
        Set.of(Roles.FOURNISSEUR),
        Set.of(),
        fournisseurId);
  }

  @Test
  void should_limiter_le_portail_au_fournisseur_du_jeton() {
    Fournisseur f = ctx.enregistrer(Fixtures.actif());
    CurrentUser moi = compte(f.getId());
    assertThat(portail.profil(moi).fournisseur().getId()).isEqualTo(f.getId());
    assertThat(portail.profil(moi).coordonneesBancairesVisibles()).isTrue();
    portail.publierLigne(
        moi,
        new LigneCatalogueCommand(
            "SE-PH20",
            "Pelle hydraulique 20 t",
            CategorieMateriel.MATERIEL_ROULANT,
            BigDecimal.valueOf(95_000_000),
            "unité",
            60));
    assertThat(portail.catalogue(moi))
        .singleElement()
        .extracting(l -> l.getReference())
        .isEqualTo("SE-PH20");
    var modifie =
        portail.modifierProfil(
            moi, 0, Fixtures.commande("Sahel Engins SA (portail)", "005123456 2G3"));
    assertThat(modifie.fournisseur().getProfil().raisonSociale())
        .isEqualTo("Sahel Engins SA (portail)");
  }

  @Test
  void should_refuser_403_when_compte_sans_fournisseur() {
    CurrentUser sansClaim = compte(null);
    assertThatThrownBy(() -> portail.profil(sansClaim))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("fournisseur_id");
  }
}

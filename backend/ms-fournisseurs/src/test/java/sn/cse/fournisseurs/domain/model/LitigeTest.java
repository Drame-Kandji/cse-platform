package sn.cse.fournisseurs.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.fournisseurs.support.Fixtures.MAINTENANT;
import static sn.cse.fournisseurs.support.Fixtures.RA;

import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.fournisseurs.support.Fixtures;

class LitigeTest {

  private static Litige ouvrir() {
    return Litige.ouvrir(
        "LIT-2026-001",
        Fixtures.actif(),
        null,
        " Livraison incomplète ",
        "2 camions sur 5",
        GraviteLitige.MOYENNE,
        RA,
        MAINTENANT);
  }

  @Test
  void should_ouvrir_avec_entree_ouverture() {
    Litige l = ouvrir();
    assertThat(l.getStatut()).isEqualTo(StatutLitige.OUVERT);
    assertThat(l.getObjet()).isEqualTo("Livraison incomplète");
    assertThat(l.getRaisonSociale()).isEqualTo("Sahel Engins SA");
    assertThat(l.getActions())
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.type()).isEqualTo(TypeActionLitige.OUVERTURE);
              assertThat(a.auteurNom()).isEqualTo("Fatou Sarr");
            });
    assertThat(l.getDateResolution()).isEmpty();
    assertThat(l.getContratId()).isEmpty();
  }

  @Test
  void should_passer_en_cours_puis_resolu() {
    Litige l = ouvrir();
    assertThat(l.agir(TypeActionLitige.ACTION, "Relance", RA, MAINTENANT.plusSeconds(60)))
        .isFalse();
    assertThat(l.getStatut()).isEqualTo(StatutLitige.EN_COURS);
    assertThat(l.agir(TypeActionLitige.ACTION, "Seconde relance", RA, MAINTENANT.plusSeconds(90)))
        .isFalse();
    assertThat(
            l.agir(TypeActionLitige.RESOLUTION, "Avoir accepté", RA, MAINTENANT.plusSeconds(120)))
        .isTrue();
    assertThat(l.getStatut()).isEqualTo(StatutLitige.RESOLU);
    assertThat(l.getDateResolution()).contains(MAINTENANT.plusSeconds(120));
    assertThat(l.getActions())
        .extracting(ActionLitige::type)
        .containsExactly(
            TypeActionLitige.OUVERTURE,
            TypeActionLitige.ACTION,
            TypeActionLitige.ACTION,
            TypeActionLitige.RESOLUTION);
  }

  @Test
  void should_classer_directement() {
    Litige l = ouvrir();
    assertThat(l.agir(TypeActionLitige.CLASSEMENT, "Sans suite", RA, MAINTENANT)).isTrue();
    assertThat(l.getStatut()).isEqualTo(StatutLitige.CLASSE);
  }

  @Test
  void should_refuser_action_with_409_when_litige_clos() {
    Litige l = ouvrir();
    l.agir(TypeActionLitige.RESOLUTION, "ok", RA, MAINTENANT);
    assertThatThrownBy(() -> l.agir(TypeActionLitige.ACTION, "trop tard", RA, MAINTENANT))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("LIT-2026-001");
  }

  @Test
  void should_refuser_action_ouverture_et_commentaire_vide() {
    Litige l = ouvrir();
    assertThatThrownBy(() -> l.agir(TypeActionLitige.OUVERTURE, "x", RA, MAINTENANT))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> l.agir(TypeActionLitige.ACTION, " ", RA, MAINTENANT))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_exiger_objet_et_description() {
    assertThatThrownBy(
            () ->
                Litige.ouvrir(
                    "LIT-1",
                    Fixtures.actif(),
                    null,
                    null,
                    "d",
                    GraviteLitige.FAIBLE,
                    RA,
                    MAINTENANT))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                Litige.ouvrir(
                    "LIT-1",
                    Fixtures.actif(),
                    null,
                    "o",
                    " ",
                    GraviteLitige.FAIBLE,
                    RA,
                    MAINTENANT))
        .isInstanceOf(BusinessRuleException.class);
  }
}
